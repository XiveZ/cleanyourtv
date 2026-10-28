package local.tvhome.netspeed;

import android.content.Context;
import android.content.SharedPreferences;

/** on, interval_ms (Rate.INTERVALS_MS), bits (false: bytes). */
final class Prefs {
    private Prefs() { }

    static SharedPreferences of(Context ctx) {
        return ctx.getSharedPreferences("netspeed", Context.MODE_PRIVATE);
    }

    static boolean on(Context ctx) {
        return of(ctx).getBoolean("on", false);
    }

    static int intervalMs(Context ctx) {
        int ms = of(ctx).getInt("interval_ms", 1000);
        for (int allowed : Rate.INTERVALS_MS) if (allowed == ms) return ms;
        return 1000;
    }

    static boolean bits(Context ctx) {
        return of(ctx).getBoolean("bits", false);
    }
}
