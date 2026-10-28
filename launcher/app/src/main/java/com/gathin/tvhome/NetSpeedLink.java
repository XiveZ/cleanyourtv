package com.gathin.tvhome;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

/**
 * Launcher settings' side of the Network speed add-on (addons/net-speed): reads and changes its settings through
 * its provider (signature permission). The add-on draws the line; the launcher only passes the choices on.
 */
final class NetSpeedLink {
    private NetSpeedLink() { }

    private static final Uri URI = Uri.parse("content://local.tvhome.netspeed.control");

    /** on, interval_ms, bits and overlay, or null when the add-on is not installed. */
    static Bundle get(Context ctx) {
        return call(ctx, "get", null, null);
    }

    static void set(Context ctx, String key, Object value) {
        Bundle extras = new Bundle();
        if (value instanceof Boolean) extras.putBoolean("value", (Boolean) value);
        else if (value instanceof Integer) extras.putInt("value", (Integer) value);
        else return;
        call(ctx, "set", key, extras);
    }

    private static Bundle call(Context ctx, String method, String arg, Bundle extras) {
        try {
            return ctx.getContentResolver().call(URI, method, arg, extras);
        } catch (RuntimeException e) {
            return null;   // not installed, or not signed like this launcher
        }
    }
}
