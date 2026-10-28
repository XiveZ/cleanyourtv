package com.gathin.tvhome;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Puts the dock and the grid away and brings them back: dock_mode autohide (hides when idle) and drawer (a ^ mark
 * while away), and an empty dock, which shows no dock until Down opens the grid with every app (Android's drawer).
 * Home keeps the tiles; this class only owns whether they are on screen.
 */
final class DockVisibility {

    /** What a key does to the dock (decide()). */
    enum KeyAction { PASS, TO_HEADER, REVEAL, HIDE }

    /** What this class needs from Home. */
    interface Host {
        boolean resumed();
        /** A menu, the app library or reorder mode is open: the dock stays. */
        boolean busy();
        /** Every app was taken out of the dock. */
        boolean dockEmpty();
        int lastFocused();
        void focusTile(int position);
        /** The highlight was hidden while focus or scroll moved: place it on the focused tile again. */
        void replaceHighlight();
    }

    private final Host host;
    private final FrameLayout root;
    private final RecyclerView grid;
    private final View glass, hero, ambient, header;
    private final boolean heroShown, ambientShown, animate;
    private final int columns;
    private final String mode;
    private final long hideMs;
    /** drawer: the ^ mark at the bottom while the dock is away. */
    private final View drawerHint;
    private final Runnable hideLater = () -> hide(true);
    private boolean hidden;
    /** The key whose down brought the dock back: its up is swallowed too, so OK does not open the tile under it. */
    private int swallowKeyUp = -1;
    /** dockEmpty() at the last tile rebuild: becoming empty puts the grid away at once. */
    private boolean wasEmpty;

    DockVisibility(Host host, FrameLayout root, RecyclerView grid, View glass, View hero, View ambient, View header,
                   Config cfg, int columns, boolean animate) {
        this.host = host;
        this.root = root;
        this.grid = grid;
        this.glass = glass;
        this.hero = hero;
        this.ambient = ambient;
        this.header = header;
        this.heroShown = cfg.dockTitle;
        this.ambientShown = cfg.ambientGlow;
        this.columns = columns;
        this.animate = animate;
        this.mode = cfg.dockMode;
        this.hideMs = cfg.dockHideSeconds * 1000L;
        this.drawerHint = Config.DOCK_DRAWER.equals(mode) ? addDrawerHint() : null;
    }

    /**
     * The key's effect, from what is on screen. Away: arrows and OK on the grid bring the dock back, except Up, which
     * goes to the header; from the header only Down does. Shown: Back on the grid puts it away unless a menu is open.
     */
    static KeyAction decide(boolean hidden, boolean inGrid, int code, boolean busy) {
        if (hidden) {
            boolean ok = code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER
                    || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
            boolean arrow = code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                    || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT;
            if (!ok && !arrow) return KeyAction.PASS;
            if (!inGrid) return code == KeyEvent.KEYCODE_DPAD_DOWN ? KeyAction.REVEAL : KeyAction.PASS;
            return code == KeyEvent.KEYCODE_DPAD_UP ? KeyAction.TO_HEADER : KeyAction.REVEAL;
        }
        boolean back = code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_ESCAPE;
        return back && inGrid && !busy ? KeyAction.HIDE : KeyAction.PASS;
    }

    private boolean timed() {
        return !Config.DOCK_ALWAYS.equals(mode);
    }

    /** The grid can be put away: a dock_mode that hides it, or an empty dock. */
    boolean hides() {
        return timed() || host.dockEmpty();
    }

    /** Any key on Home: the idle countdown starts again. */
    void onUserInteraction() {
        if (!hidden) schedule();
    }

    void onResume() {
        if (hides()) hide(false);
    }

    void onPause() {
        root.removeCallbacks(hideLater);
    }

    /** After the tiles were rebuilt: a dock that just became empty goes away at once. */
    void onTilesRebuilt() {
        boolean empty = host.dockEmpty();
        if (empty == wasEmpty) return;
        wasEmpty = empty;
        if (empty && host.resumed()) hide(false);
    }

    /** True when the key was used here. */
    boolean onKey(KeyEvent event) {
        if (!hides()) return false;
        int code = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (!down && code == swallowKeyUp) {
            swallowKeyUp = -1;
            return true;
        }
        boolean inGrid = grid.hasFocus();
        switch (decide(hidden, inGrid, code, host.busy())) {
            case TO_HEADER:
                if (down && event.getRepeatCount() == 0 && header.getVisibility() == View.VISIBLE) header.requestFocus();
                return true;
            case REVEAL:
                if (down) {
                    if (event.getRepeatCount() == 0) reveal(inGrid);
                    swallowKeyUp = code;
                }
                return true;
            case HIDE:
                if (!down && !event.isCanceled()) {
                    if (!host.dockEmpty()) {
                        grid.scrollToPosition(0);
                        host.focusTile(dockPlace());
                    }
                    hide(true);
                }
                return true;
            default:
                return false;
        }
    }

    private void reveal(boolean inGrid) {
        show();
        if (host.dockEmpty()) {
            if (!inGrid || host.lastFocused() < columns) host.focusTile(columns);   // the first app of the grid
        } else if (!inGrid) {
            host.focusTile(dockPlace());
        }
    }

    /** The dock place focus goes back to: the last one used, or the first. */
    private int dockPlace() {
        return Math.max(0, Math.min(host.lastFocused(), columns - 1));
    }

    private void schedule() {
        root.removeCallbacks(hideLater);
        if (timed() && !hidden && host.resumed()) root.postDelayed(hideLater, hideMs);
    }

    /** Slides the dock and the grid away; waits instead while a menu, the library or reorder mode is open. */
    void hide(boolean animated) {
        root.removeCallbacks(hideLater);
        if (!hides() || hidden) return;
        if (host.busy()) {
            schedule();
            return;
        }
        hidden = true;
        float away = Math.max(root.getHeight(), root.getResources().getDisplayMetrics().heightPixels);
        grid.animate().cancel();
        setDecorations(View.INVISIBLE);
        if (animated && animate) {
            grid.animate().translationY(away * 0.35f).alpha(0f).setDuration(240)
                    .setInterpolator(new AccelerateInterpolator(1.4f))
                    .withEndAction(() -> grid.setTranslationY(away)).start();
        } else {
            grid.setAlpha(0f);
            grid.setTranslationY(away);
        }
        if (drawerHint != null) {
            drawerHint.setVisibility(View.VISIBLE);
            drawerHint.setAlpha(0f);
            drawerHint.animate().alpha(1f).setDuration(animated ? 300 : 0).start();
        }
    }

    /** Brings the dock back (reorder mode and Down use it). */
    void show() {
        if (!hidden) return;
        hidden = false;
        grid.animate().cancel();
        if (drawerHint != null) drawerHint.setVisibility(View.GONE);
        Runnable shown = () -> {
            if (hidden) return;
            host.replaceHighlight();
            setDecorations(View.VISIBLE);
        };
        if (animate) {
            grid.setTranslationY(grid.getHeight() * 0.25f);
            grid.animate().translationY(0f).alpha(1f).setDuration(260)
                    .setInterpolator(new DecelerateInterpolator(1.6f))
                    .withEndAction(() -> grid.postDelayed(shown, 120)).start();
        } else {
            grid.setTranslationY(0f);
            grid.setAlpha(1f);
            grid.post(shown);
        }
        schedule();
    }

    /** The highlight, the dock title and the glow go with the dock. */
    private void setDecorations(int visibility) {
        glass.setVisibility(visibility);
        if (heroShown) hero.setVisibility(visibility);
        if (ambientShown) ambient.setVisibility(visibility);
    }

    private View addDrawerHint() {
        float density = root.getResources().getDisplayMetrics().density;
        ImageView mark = new ImageView(root.getContext());
        mark.setImageResource(R.drawable.ic_drawer_up);
        mark.setScaleType(ImageView.ScaleType.CENTER);
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0x40FFFFFF);
        pill.setStroke(Math.round(density), 0x59FFFFFF);
        pill.setCornerRadius(18 * density);
        mark.setBackground(pill);
        mark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.round(72 * density), Math.round(36 * density),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Math.round(28 * density);
        mark.setVisibility(View.GONE);
        root.addView(mark, lp);
        return mark;
    }
}
