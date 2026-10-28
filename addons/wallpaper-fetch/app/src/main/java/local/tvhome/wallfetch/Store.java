package local.tvhome.wallfetch;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

/** The add-on's settings and state, and the folder Home reads the pictures from. */
final class Store {
    private Store() { }

    static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences("wallfetch", Context.MODE_PRIVATE);
    }

    static int days(Context ctx) {
        return prefs(ctx).getInt("days", 30);
    }

    static int count(Context ctx) {
        return prefs(ctx).getInt("count", 40);
    }

    /** When the pictures were last replaced (also the version Home compares), or 0. */
    static long last(Context ctx) {
        return prefs(ctx).getLong("last", 0);
    }

    /** The finished set Home reads. */
    static File walls(Context ctx) {
        return new File(ctx.getFilesDir(), "walls");
    }

    /** A set being downloaded; it replaces walls/ only once it is complete. */
    static File incoming(Context ctx) {
        return new File(ctx.getFilesDir(), "incoming");
    }

    static File[] pictures(File dir) {
        File[] files = dir.listFiles((d, name) -> name.endsWith(".jpg"));
        return files == null ? new File[0] : files;
    }
}
