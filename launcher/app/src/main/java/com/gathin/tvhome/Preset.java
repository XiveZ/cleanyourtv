package com.gathin.tvhome;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * A look switched on from Launcher settings: a set of on-TV choices laid over the current ones, with the choices it
 * replaced kept aside, so switching back gives exactly what was there. Works on the tv_overrides object.
 */
final class Preset {
    private Preset() { }

    private static final String ACTIVE = "_preset", BACKUP = "_preset_backup";

    /** "claude": warm charcoal and clay, glass with a rim, dark pills under the widgets, its own wallpaper. */
    static JSONObject values(String name) throws JSONException {
        if (!"claude".equals(name)) return null;
        return new JSONObject()
                .put("background_color", "#141413")
                .put("preset_wallpaper", ClaudeWallpaper.FILE_NAME)
                .put("wallpaper_dim", 0)
                .put("glass_style", "rim")
                .put("dock_glass", true)
                .put("dock_band", true)
                .put("app_glow", false)
                .put("ambient_glow", false)
                .put("header_white", true)
                .put("clock_glow", false)
                .put("widget_backdrop", true)
                .put("widget_gap", 12)
                .put("always_show_names", true)
                .put("name_weight", "medium");
    }

    static String active(JSONObject overrides) {
        return overrides.optString(ACTIVE, "");
    }

    static JSONObject apply(JSONObject overrides, String name) throws JSONException {
        JSONObject preset = values(name);
        if (preset == null) return overrides;
        JSONObject out = new JSONObject(overrides.toString());
        if (!active(out).isEmpty()) out = restore(out);   // a second apply keeps the first backup
        JSONObject backup = new JSONObject();
        for (Iterator<String> keys = preset.keys(); keys.hasNext(); ) {
            String key = keys.next();
            backup.put(key, out.has(key) ? out.get(key) : JSONObject.NULL);
            out.put(key, preset.get(key));
        }
        return out.put(ACTIVE, name).put(BACKUP, backup);
    }

    static JSONObject restore(JSONObject overrides) throws JSONException {
        JSONObject out = new JSONObject(overrides.toString());
        JSONObject backup = out.optJSONObject(BACKUP);
        if (backup != null) {
            for (Iterator<String> keys = backup.keys(); keys.hasNext(); ) {
                String key = keys.next();
                Object was = backup.get(key);
                if (was == JSONObject.NULL) out.remove(key);
                else out.put(key, was);
            }
        }
        out.remove(BACKUP);
        out.remove(ACTIVE);
        return out;
    }
}
