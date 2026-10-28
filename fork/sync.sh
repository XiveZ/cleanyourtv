#!/usr/bin/env bash
# Fork only (FORK.md): bring the newest upstream into this fork's branch without breaking it.
#   fork/sync.sh            merge upstream/main into the current branch, then build and test
#   fork/sync.sh --no-check merge only
# Never pushes. Needs a clean working tree. A README.md conflict is resolved here: upstream's README is taken and
# the fork's block (<!-- fork:start --> ... <!-- fork:end -->) is put back on top. Any other conflict stops the
# merge with the list of files; `git merge --abort` returns everything as it was.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

check=1
[ "${1:-}" = "--no-check" ] && check=0

# The upstream remote: one named "upstream", else whichever points at Gathin23/cleanyourtv.
remote=upstream
if ! git remote get-url upstream >/dev/null 2>&1; then
    remote=$(git remote -v | awk '/Gathin23\/cleanyourtv(\.git)? \(fetch\)/ {print $1; exit}')
fi
if [ -z "$remote" ]; then
    echo "No upstream remote. Add it once:  git remote add upstream https://github.com/Gathin23/cleanyourtv.git" >&2
    exit 1
fi
if [ -n "$(git status --porcelain --untracked-files=no)" ]; then
    echo "The working tree has changes. Commit or stash them first." >&2
    exit 1
fi

branch=$(git rev-parse --abbrev-ref HEAD)
backup="fork-backup-$(date +%Y%m%d-%H%M)"
git fetch "$remote"
git branch -f "$backup" HEAD
echo "Backup of $branch: $backup"

if git merge --no-ff --no-edit "$remote/main"; then
    echo "Merged $remote/main into $branch."
else
    conflicts=$(git diff --name-only --diff-filter=U)
    if [ "$conflicts" = "README.md" ]; then
        # Upstream's README with the fork's block back on top.
        ours=$(mktemp) theirs=$(mktemp)
        trap 'rm -f "$ours" "$theirs"' EXIT
        git show HEAD:README.md > "$ours"
        git show MERGE_HEAD:README.md > "$theirs"
        python3 - "$ours" "$theirs" > README.md <<'PY'
import sys
ours, theirs = (open(p, encoding="utf-8").read() for p in sys.argv[1:3])
start, end = ours.find("<!-- fork:start -->"), ours.find("<!-- fork:end -->")
if start < 0 or end < 0:
    sys.exit("the fork block is missing from our README.md")
block = ours[start:end + len("<!-- fork:end -->")] + "\n\n"
sys.stdout.write(block + theirs)
PY
        git add README.md
        git commit --no-edit
        echo "README.md: upstream's text with the fork block on top."
    else
        echo "Conflicts to resolve by hand (see FORK.md, Sync):" >&2
        echo "$conflicts" | sed 's/^/  /' >&2
        echo "Resolve, git add, git commit; or undo with: git merge --abort" >&2
        exit 1
    fi
fi

[ "$check" = 1 ] || exit 0

echo "== Launcher: unit tests, lint, build"
(cd launcher && ./gradlew --no-daemon -q testReleaseUnitTest lintRelease assembleRelease)
for addon in wallpaper-fetch net-speed; do
    echo "== Add-on $addon: build and tests"
    (cd "addons/$addon" && ./gradlew --no-daemon -q assembleRelease testReleaseUnitTest)
done
echo "== Repository tests (the fork's known differences fail on purpose, FORK.md)"
py=tools/venv/bin/python
[ -x "$py" ] || py=python3
"$py" -m unittest discover -s tests 2>&1 | grep -E "^(FAIL|ERROR):|^Ran|^OK|^FAILED" || true
echo
echo "Done. Look at the result, then:  git push origin $branch   (the backup is $backup)"
