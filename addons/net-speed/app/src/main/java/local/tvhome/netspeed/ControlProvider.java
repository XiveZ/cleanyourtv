package local.tvhome.netspeed;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

/**
 * Launcher settings change the line through here (permission: signature). call("get") gives on, interval_ms, bits
 * and overlay (may draw over apps); call("set", key, {"value": ...}) saves one and starts or stops the line.
 */
public final class ControlProvider extends ContentProvider {
    private static final String PERMISSION = "local.tvhome.netspeed.CONTROL";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        getContext().enforceCallingPermission(PERMISSION, "network speed settings");   // call() is not covered by the manifest
        if ("set".equals(method) && arg != null && extras != null) {
            switch (arg) {
                case "on":
                case "bits":
                    Prefs.of(getContext()).edit().putBoolean(arg, extras.getBoolean("value")).commit();
                    break;
                case "interval_ms":
                    Prefs.of(getContext()).edit().putInt(arg, extras.getInt("value")).commit();
                    break;
                default:
                    return null;
            }
            SpeedService.apply(getContext());
        } else if (!"get".equals(method)) {
            return null;
        }
        Bundle b = new Bundle();
        b.putBoolean("on", Prefs.on(getContext()));
        b.putInt("interval_ms", Prefs.intervalMs(getContext()));
        b.putBoolean("bits", Prefs.bits(getContext()));
        b.putBoolean("overlay", Settings.canDrawOverlays(getContext()));
        return b;
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
