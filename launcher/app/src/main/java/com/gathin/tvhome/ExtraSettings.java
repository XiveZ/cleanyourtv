package com.gathin.tvhome;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;



/**
 * The extra sections of Launcher settings: theme keys changed from the remote. Each row shows "Name: ‹ value ›";
 * OK and Right step to the next value, Left to the one before. A change is saved at once as an on-TV choice
 * (Config.TV_OVERRIDES_KEY) and Home rebuilds with it when it comes back.
 */
final class ExtraSettings {
    private final Activity activity;
    private final LinearLayout box;
    private final Config config;
    private final Runnable changed;
    /** Closes the settings and opens the module cards list on Home. */
    private final Runnable openCards;

    ExtraSettings(Activity activity, LinearLayout box, Runnable changed, Runnable openCards) {
        this.activity = activity;
        this.box = box;
        this.changed = changed;
        this.openCards = openCards;
        this.config = Config.load(activity);
    }

    void build() {
        Config c = config;
        section(R.string.xs_section_wallpaper);
        option("wallpaper_rotation_minutes", R.string.xs_wallpaper_every, c.wallpaperRotationMinutes,
                new Object[]{0, 5, 15, 30, 60, 120, 180, 360, 720, 1440}, this::minutes);
        bool("wallpaper_align", R.string.xs_wallpaper_align, c.wallpaperAlign);
        option("wallpaper_order", R.string.xs_wallpaper_order, c.wallpaperOrder,
                new Object[]{WallpaperRotation.ORDER_SHUFFLE, WallpaperRotation.ORDER_SEQUENTIAL},
                v -> text(WallpaperRotation.ORDER_SEQUENTIAL.equals(v) ? R.string.xs_order_sequential : R.string.xs_order_shuffle));
        option("wallpaper_dim", R.string.xs_wallpaper_dim, c.wallpaperDim,
                new Object[]{0, 15, 25, 35, 50, 65}, v -> v + "%");

        section(R.string.xs_section_dock);
        option("dock_mode", R.string.xs_dock_mode, c.dockMode,
                new Object[]{Config.DOCK_ALWAYS, Config.DOCK_AUTOHIDE, Config.DOCK_DRAWER},
                v -> text(Config.DOCK_AUTOHIDE.equals(v) ? R.string.xs_dock_autohide
                        : Config.DOCK_DRAWER.equals(v) ? R.string.xs_dock_drawer : R.string.xs_dock_always));
        option("dock_hide_seconds", R.string.xs_dock_hide_after, c.dockHideSeconds,
                new Object[]{3, 5, 10, 15, 30, 60, 120}, this::seconds);
        bool("hide_grid_under_dock", R.string.xs_hide_grid, c.hideGridUnderDock);
        bool("fill_dock", R.string.xs_fill_dock, c.fillDock);
        bool("single_row", R.string.xs_single_row, c.singleRow);
        bool("dock_band", R.string.xs_dock_band, c.dockBand);
        bool("dock_glass", R.string.xs_dock_glass, c.dockGlass);
        hint(R.string.xs_dock_hint);

        section(R.string.xs_section_names);
        bool("always_show_names", R.string.xs_always_names, c.alwaysShowNames);
        option("name_size", R.string.xs_name_size, c.nameSize,
                new Object[]{12, 13, 14, 15, 16, 18}, v -> v + " sp");
        option("name_weight", R.string.xs_name_weight, c.nameWeight,
                new Object[]{"regular", "medium", "bold"},
                v -> text("regular".equals(v) ? R.string.xs_weight_regular
                        : "bold".equals(v) ? R.string.xs_weight_bold : R.string.xs_weight_medium));
        option("name_spacing", R.string.xs_name_spacing, c.nameSpacing,
                new Object[]{0, 2, 5, 8, 12}, v -> String.valueOf((Integer) v / 100f));
        bool("uniform_tiles", R.string.xs_uniform_tiles, c.uniformTiles);

        section(R.string.xs_section_effects);
        bool("app_glow", R.string.xs_app_glow, c.appGlow);
        bool("ambient_glow", R.string.xs_ambient_glow, c.ambientGlow);
        bool("fade_rows", R.string.xs_fade_rows, c.fadeRows);
        bool("focus_overshoot", R.string.xs_focus_overshoot, c.focusOvershoot);
        bool("reduced_motion", R.string.xs_reduced_motion, c.reducedMotion);

        section(R.string.xs_section_widgets);
        bool("show_greeting", R.string.xs_greeting, c.showGreeting);
        option("date_below_clock", R.string.xs_date_place, c.dateBelowClock, new Object[]{false, true},
                v -> text((Boolean) v ? R.string.xs_date_below : R.string.xs_date_beside));
        bool("show_network_pill", R.string.xs_network_pill, c.showNetworkPill);
        bool("show_inputs_pill", R.string.xs_inputs_pill, c.showInputsPill);
        bool("show_stats", R.string.xs_stats, c.showStats);
        option("stats_refresh_seconds", R.string.xs_stats_every, c.statsRefreshSeconds,
                new Object[]{2, 5, 10, 30, 60}, this::seconds);
        option("home_idle_clock_minutes", R.string.xs_idle_clock, c.homeIdleClockMinutes,
                new Object[]{0, 1, 5, 10, 15, 30, 60}, this::minutes);
        bool("clock_wallpaper", R.string.xs_clock_wallpaper, c.clockWallpaper);
        bool("clock_glow", R.string.xs_clock_glow, c.clockGlow);
        bool("header_white", R.string.xs_header_white, c.headerWhite);
        bool("icon_only_buttons", R.string.xs_icon_only_buttons, c.iconOnlyButtons);
        bool("wifi_status_color", R.string.xs_wifi_colour, c.wifiStatusColor);
        bool("dock_title", R.string.xs_dock_title, c.dockTitle);
        option("glass_style", R.string.xs_glass_style, c.glassStyle,
                new Object[]{Config.GLASS_DEFAULT, Config.GLASS_RIM, Config.GLASS_HALO},
                v -> text(Config.GLASS_RIM.equals(v) ? R.string.xs_glass_rim
                        : Config.GLASS_HALO.equals(v) ? R.string.xs_glass_halo : R.string.xs_glass_glass));
        bool("launcher_settings_button", R.string.xs_gear_opens_launcher, c.launcherSettingsButton);

        section(R.string.xs_section_addons);
        int installed = activity.getPackageManager().queryIntentActivities(new android.content.Intent(
                android.content.Intent.ACTION_MAIN).addCategory("com.gathin.tvhome.category.DOCK_CARD"), 0).size();
        action(R.string.xs_addons_manage, openCards);
        bool("module_cards", R.string.xs_addons_on, c.moduleCards);
        hint(activity.getString(R.string.xs_addons_hint, installed));
    }

    // ---- rows ------------------------------------------------------------------------------------

    private interface Label {
        String of(Object value);
    }

    private String text(int id) {
        return activity.getString(id);
    }

    private String minutes(Object v) {
        int m = (Integer) v;
        if (m == 0) return text(R.string.off);
        if (m % 60 == 0) return activity.getString(R.string.xs_hours, m / 60);
        return activity.getString(R.string.xs_minutes, m);
    }

    private String seconds(Object v) {
        return activity.getString(R.string.xs_seconds, (Integer) v);
    }

    private void section(int title) {
        TextView t = new TextView(activity);
        t.setText(title);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(30);
        box.addView(t, lp);
    }

    private void hint(int text) {
        hint(activity.getString(text));
    }

    private void hint(String text) {
        TextView t = new TextView(activity);
        t.setText(text);
        t.setTextColor(0xFFACB8C1);
        t.setTextSize(14);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(7);
        box.addView(t, lp);
    }

    private TextView row() {
        TextView row = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_settings_option, box, false);
        box.addView(row);
        return row;
    }

    private void action(int title, Runnable run) {
        TextView row = row();
        row.setText(title);
        row.setOnClickListener(v -> run.run());
    }

    private void bool(String key, int title, boolean current) {
        option(key, title, current, new Object[]{true, false}, v -> text((Boolean) v ? R.string.on : R.string.off));
    }

    private void option(String key, int title, Object current, Object[] values, Label label) {
        TextView row = row();
        int[] at = {indexOf(values, current)};
        String name = text(title);
        Runnable show = () -> row.setText(name + ":   ‹ "
                + (at[0] >= 0 ? label.of(values[at[0]]) : String.valueOf(current)) + " ›");
        show.run();
        java.util.function.IntConsumer step = by -> {
            at[0] = at[0] < 0 ? 0 : (at[0] + by + values.length) % values.length;
            Config.putTvOverride(activity, key, values[at[0]]);
            changed.run();
            show.run();
        };
        row.setOnClickListener(v -> step.accept(1));
        row.setOnKeyListener((v, code, event) -> {
            if (code != KeyEvent.KEYCODE_DPAD_LEFT && code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
            if (event.getAction() == KeyEvent.ACTION_DOWN) step.accept(code == KeyEvent.KEYCODE_DPAD_RIGHT ? 1 : -1);
            return true;
        });
    }

    private static int indexOf(Object[] values, Object current) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return i;
        return -1;
    }

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
