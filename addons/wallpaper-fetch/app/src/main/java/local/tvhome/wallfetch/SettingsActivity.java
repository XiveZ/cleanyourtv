package local.tvhome.wallfetch;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/** The card: how often to update, how many pictures, Update now, and what happened last. */
public final class SettingsActivity extends Activity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView status;
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            showStatus();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        FetchJob.schedule(this);   // idempotent: (re)sets the periodic job to the saved period
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFF101419);
        int pad = dp(64);
        page.setPadding(pad, dp(48), pad, dp(32));
        TextView title = text(getString(R.string.title), 32, 0xFFFFFFFF);
        page.addView(title);
        page.addView(cycle(R.string.every, Schedule.DAYS, "days", Store.days(this), this::daysLabel, FetchJob::schedule));
        page.addView(cycle(R.string.count, Schedule.COUNTS, "count", Store.count(this), String::valueOf, c -> { }));
        TextView now = row(getString(R.string.now));
        now.setOnClickListener(v -> {
            FetchJob.runNow(this);
            Store.prefs(this).edit().putBoolean("running", true).putInt("got", 0).putInt("of", Store.count(this)).apply();
            showStatus();
        });
        page.addView(now);
        status = text("", 18, 0xFFDDE3E8);
        status.setPadding(0, dp(24), 0, 0);
        page.addView(status);
        TextView note = text(getString(R.string.note), 15, 0xFFACB8C1);
        note.setPadding(0, dp(16), 0, 0);
        page.addView(note);
        setContentView(page);
        page.getChildAt(1).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private interface Label {
        String of(int value);
    }

    private interface Saved {
        void run(android.content.Context ctx);
    }

    /** "Name:  < value >": OK and Right step forward, Left back; saved at once. */
    private TextView cycle(int name, int[] values, String key, int current, Label label, Saved saved) {
        TextView row = row("");
        int[] at = {0};
        for (int i = 0; i < values.length; i++) if (values[i] == current) at[0] = i;
        Runnable show = () -> row.setText(getString(name) + ":   ‹ " + label.of(values[at[0]]) + " ›");
        show.run();
        java.util.function.IntConsumer step = by -> {
            at[0] = (at[0] + by + values.length) % values.length;
            Store.prefs(this).edit().putInt(key, values[at[0]]).apply();
            saved.run(this);
            show.run();
            showStatus();
        };
        row.setOnClickListener(v -> step.accept(1));
        row.setOnKeyListener((v, code, event) -> {
            if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) step.accept(code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
            return true;
        });
        return row;
    }

    private String daysLabel(int days) {
        switch (days) {
            case 0: return getString(R.string.off);
            case 7: return getString(R.string.week);
            case 30: return getString(R.string.month);
            case 365: return getString(R.string.year);
            default: return getString(R.string.days, days);
        }
    }

    private void showStatus() {
        android.content.SharedPreferences p = Store.prefs(this);
        String text;
        if (p.getBoolean("running", false)) {
            text = getString(R.string.running, p.getInt("got", 0), p.getInt("of", Store.count(this)));
        } else if (Store.last(this) > 0) {
            int days = Store.days(this);
            String next = days == 0 ? getString(R.string.off) : date(Store.last(this) + Schedule.periodMs(days));
            text = getString(R.string.last, date(Store.last(this)), p.getInt("have", 0), next);
        } else {
            text = getString(R.string.never);
        }
        String error = p.getString("error", null);
        if (error != null && !p.getBoolean("running", false)) text += "\n" + getString(R.string.failed, error);
        status.setText(text);
    }

    private String date(long ms) {
        return DateFormat.getMediumDateFormat(this).format(new Date(ms)) + " "
                + DateFormat.getTimeFormat(this).format(new Date(ms));
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private TextView row(String s) {
        TextView t = text(s, 20, 0xFFFFFFFF);
        t.setFocusable(true);
        t.setClickable(true);
        t.setPadding(dp(20), 0, dp(20), 0);
        t.setGravity(android.view.Gravity.CENTER_VERTICAL);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, pill(0xFF2E4A3E, 0xFFB8E0C8));
        bg.addState(new int[]{}, pill(0xFF1E252D, 0));
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        lp.topMargin = dp(14);
        t.setLayoutParams(lp);
        return t;
    }

    private GradientDrawable pill(int fill, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(14));
        if (stroke != 0) d.setStroke(dp(2), stroke);
        return d;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
