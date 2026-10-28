package com.gathin.tvhome;

import android.app.Activity;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * What Launcher settings offer, as data (SettingsModel): grouped into general categories, the widgets, and the rest.
 * Each row says where its value is saved: an on-TV theme choice (Config.TV_OVERRIDES_KEY), one of the launcher's
 * own preference keys, or the Network speed add-on.
 */
final class SettingsCatalog {
    /** What the rows need from the settings screen. */
    interface Host {
        /** A choice was saved: Home rebuilds when it comes back. */
        void changed();
        void openGallery();
        void reorder();
        void openCards();
        void widgetPlaces();
        void preview();
        boolean openOnStartAvailable();
        void openOnStart();
        String openOnStartLabel();
        void defaultHome();
        void tvSettings();
        void followTheme();
        /** advanced_effects is switched on only after a warning; false means wait for the answer. */
        void confirmAdvanced(Runnable yes);
    }

    private final Activity a;
    private final Host host;
    private final Config c;
    private final SharedPreferences prefs;

    SettingsCatalog(Activity activity, Host host) {
        this.a = activity;
        this.host = host;
        this.c = Config.load(activity);
        this.prefs = activity.getSharedPreferences(Config.SETTINGS_NAME, Activity.MODE_PRIVATE);
    }

    List<SettingsModel.Category> build() {
        List<SettingsModel.Category> all = new ArrayList<>();
        all.add(new SettingsModel.Category(R.string.sc_head_general, true));
        all.add(look());
        all.add(dock());
        all.add(wallpaper());
        all.add(tiles());
        all.add(effects());
        all.add(header());
        all.add(new SettingsModel.Category(R.string.sc_head_widgets, true));
        all.add(widgetsCommon());
        all.add(new SettingsModel.Category(R.string.sc_astro, false)
                .add(bool("show_astro", R.string.xs_astro, R.string.sd_astro, c.showAstro)));
        all.add(new SettingsModel.Category(R.string.sc_stats, false)
                .add(bool("show_stats", R.string.xs_stats, R.string.sd_stats, c.showStats))
                .add(choice("stats_refresh_seconds", R.string.xs_stats_every, R.string.sd_stats_every,
                        c.statsRefreshSeconds, new Object[]{2, 5, 10, 30, 60}, this::seconds)));
        all.add(newYear());
        all.add(new SettingsModel.Category(R.string.sc_idle_clock, false)
                .add(choice("home_idle_clock_minutes", R.string.xs_idle_clock, R.string.sd_idle_clock,
                        c.homeIdleClockMinutes, new Object[]{0, 1, 5, 10, 15, 30, 60}, this::minutes))
                .add(bool("clock_wallpaper", R.string.xs_clock_wallpaper, R.string.sd_clock_wallpaper, c.clockWallpaper)));
        SettingsModel.Category net = netSpeed();
        if (net != null) all.add(net);
        all.add(new SettingsModel.Category(R.string.sc_head_more, true));
        all.add(new SettingsModel.Category(R.string.sc_addons, false)
                .add(SettingsModel.Row.action(R.string.xs_addons_manage, R.string.sd_addons_manage,
                        () -> a.getString(R.string.sv_installed, installedCards()), host::openCards))
                .add(bool("module_cards", R.string.xs_addons_on, R.string.sd_addons_on, c.moduleCards)));
        all.add(system());
        return all;
    }

    // ---- general ---------------------------------------------------------------------------------

    private SettingsModel.Category look() {
        return new SettingsModel.Category(R.string.sc_look, false)
                .add(SettingsModel.Row.choice(R.string.xs_preset, R.string.sd_preset, new Object[]{"", "claude"},
                        Preset.active(Config.tvOverrides(prefs)),
                        v -> text("claude".equals(v) ? R.string.xs_preset_claude : R.string.xs_preset_theme),
                        v -> text("claude".equals(v) ? R.string.sv_preset_claude : R.string.sv_preset_theme),
                        this::savePreset))
                .add(prefInt("columns", R.string.columns_label, R.string.sd_columns, c.columns,
                        new Object[]{4, 5, 6, 7}, String::valueOf))
                .add(prefInt(Config.ROWS_BELOW_DOCK_KEY, R.string.rows_below_dock_label, R.string.sd_rows_below,
                        c.rowsBelowDock, new Object[]{0, 1, 2, 3}, String::valueOf));
    }

    private SettingsModel.Category dock() {
        return new SettingsModel.Category(R.string.sc_dock, false)
                .add(SettingsModel.Row.choice(R.string.xs_dock_mode, R.string.sd_dock_mode,
                        new Object[]{Config.DOCK_ALWAYS, Config.DOCK_AUTOHIDE, Config.DOCK_DRAWER}, c.dockMode,
                        v -> text(Config.DOCK_AUTOHIDE.equals(v) ? R.string.xs_dock_autohide
                                : Config.DOCK_DRAWER.equals(v) ? R.string.xs_dock_drawer : R.string.xs_dock_always),
                        v -> text(Config.DOCK_AUTOHIDE.equals(v) ? R.string.sv_dock_autohide
                                : Config.DOCK_DRAWER.equals(v) ? R.string.sv_dock_drawer : R.string.sv_dock_always),
                        theme("dock_mode")))
                .add(choice("dock_hide_seconds", R.string.xs_dock_hide_after, R.string.sd_dock_hide_after,
                        c.dockHideSeconds, new Object[]{3, 5, 10, 15, 30, 60, 120}, this::seconds))
                .add(bool("hide_grid_under_dock", R.string.xs_hide_grid, R.string.sd_hide_grid, c.hideGridUnderDock))
                .add(bool("fill_dock", R.string.xs_fill_dock, R.string.sd_fill_dock, c.fillDock))
                .add(bool("single_row", R.string.xs_single_row, R.string.sd_single_row, c.singleRow))
                .add(bool("dock_band", R.string.xs_dock_band, R.string.sd_dock_band, c.dockBand))
                .add(bool("dock_glass", R.string.xs_dock_glass, R.string.sd_dock_glass, c.dockGlass))
                .add(prefInt(Config.DOCK_OPACITY_KEY, R.string.dock_opacity_label, R.string.sd_dock_opacity,
                        c.dockOpacity, new Object[]{100, 70, 50, 30}, v -> v + "%"))
                .add(bool("dock_title", R.string.xs_dock_title, R.string.sd_dock_title, c.dockTitle))
                .add(SettingsModel.Row.action(R.string.reorder, R.string.sd_reorder, null, host::reorder));
    }

    private SettingsModel.Category wallpaper() {
        return new SettingsModel.Category(R.string.sc_wallpaper, false)
                .add(SettingsModel.Row.action(R.string.wallpaper_label, R.string.sd_wallpaper_pick,
                        this::wallpaperChoice, host::openGallery))
                .add(choice("wallpaper_rotation_minutes", R.string.xs_wallpaper_every, R.string.sd_wallpaper_every,
                        c.wallpaperRotationMinutes, new Object[]{0, 5, 15, 30, 60, 120, 180, 360, 720, 1440}, this::minutes))
                .add(SettingsModel.Row.choice(R.string.xs_wallpaper_align, R.string.sd_wallpaper_align,
                        new Object[]{true, false}, c.wallpaperAlign, this::onOff,
                        v -> text((Boolean) v ? R.string.sv_align_on : R.string.sv_align_off), theme("wallpaper_align")))
                .add(SettingsModel.Row.choice(R.string.xs_wallpaper_order, R.string.sd_wallpaper_order,
                        new Object[]{WallpaperRotation.ORDER_SHUFFLE, WallpaperRotation.ORDER_SEQUENTIAL}, c.wallpaperOrder,
                        v -> text(WallpaperRotation.ORDER_SEQUENTIAL.equals(v) ? R.string.xs_order_sequential : R.string.xs_order_shuffle),
                        v -> text(WallpaperRotation.ORDER_SEQUENTIAL.equals(v) ? R.string.sv_order_sequential : R.string.sv_order_shuffle),
                        theme("wallpaper_order")))
                .add(choice("wallpaper_dim", R.string.xs_wallpaper_dim, R.string.sd_wallpaper_dim, c.wallpaperDim,
                        new Object[]{0, 15, 25, 35, 50, 65}, v -> v + "%"));
    }

    private SettingsModel.Category tiles() {
        return new SettingsModel.Category(R.string.sc_tiles, false)
                .add(bool("always_show_names", R.string.xs_always_names, R.string.sd_always_names, c.alwaysShowNames))
                .add(choice("name_size", R.string.xs_name_size, R.string.sd_name_size, c.nameSize,
                        new Object[]{12, 13, 14, 15, 16, 18}, v -> v + " sp"))
                .add(choice("name_weight", R.string.xs_name_weight, R.string.sd_name_weight, c.nameWeight,
                        new Object[]{"regular", "medium", "bold"}, v -> text("regular".equals(v) ? R.string.xs_weight_regular
                                : "bold".equals(v) ? R.string.xs_weight_bold : R.string.xs_weight_medium)))
                .add(choice("name_spacing", R.string.xs_name_spacing, R.string.sd_name_spacing, c.nameSpacing,
                        new Object[]{0, 2, 5, 8, 12}, v -> String.valueOf((Integer) v / 100f)))
                .add(bool("uniform_tiles", R.string.xs_uniform_tiles, R.string.sd_uniform_tiles, c.uniformTiles));
    }

    private SettingsModel.Category effects() {
        boolean advanced = c.animations && c.advancedEffects;
        return new SettingsModel.Category(R.string.sc_effects, false)
                .add(SettingsModel.Row.choice(R.string.xs_animations, R.string.sd_animations, new Object[]{true, false},
                        c.animations, this::onOff, null, v -> {
                            SharedPreferences.Editor e = prefs.edit().putBoolean("animations", (Boolean) v);
                            if (!(Boolean) v) e.putBoolean("advanced_effects", false);
                            e.apply();
                            host.changed();
                        }))
                .add(SettingsModel.Row.choice(R.string.advanced_effects, R.string.sd_advanced, new Object[]{true, false},
                        advanced, this::onOff, null, v -> {
                            if ((Boolean) v) host.confirmAdvanced(() -> {
                                prefs.edit().putBoolean("advanced_effects", true).apply();
                                host.changed();
                            });
                            else {
                                prefs.edit().putBoolean("advanced_effects", false).apply();
                                host.changed();
                            }
                        }))
                .add(bool("app_glow", R.string.xs_app_glow, R.string.sd_app_glow, c.appGlow))
                .add(bool("ambient_glow", R.string.xs_ambient_glow, R.string.sd_ambient_glow, c.ambientGlow))
                .add(bool("fade_rows", R.string.xs_fade_rows, R.string.sd_fade_rows, c.fadeRows))
                .add(bool("focus_overshoot", R.string.xs_focus_overshoot, R.string.sd_focus_overshoot, c.focusOvershoot))
                .add(bool("reduced_motion", R.string.xs_reduced_motion, R.string.sd_reduced_motion, c.reducedMotion))
                .add(SettingsModel.Row.choice(R.string.xs_glass_style, R.string.sd_glass_style,
                        new Object[]{Config.GLASS_DEFAULT, Config.GLASS_RIM, Config.GLASS_HALO}, c.glassStyle,
                        v -> text(Config.GLASS_RIM.equals(v) ? R.string.xs_glass_rim
                                : Config.GLASS_HALO.equals(v) ? R.string.xs_glass_halo : R.string.xs_glass_glass),
                        v -> text(Config.GLASS_RIM.equals(v) ? R.string.sv_glass_rim
                                : Config.GLASS_HALO.equals(v) ? R.string.sv_glass_halo : R.string.sv_glass_glass),
                        theme("glass_style")));
    }

    private SettingsModel.Category header() {
        return new SettingsModel.Category(R.string.sc_header, false)
                .add(bool("show_greeting", R.string.xs_greeting, R.string.sd_greeting, c.showGreeting))
                .add(SettingsModel.Row.choice(R.string.xs_date_place, R.string.sd_date_place, new Object[]{false, true},
                        c.dateBelowClock, v -> text((Boolean) v ? R.string.xs_date_below : R.string.xs_date_beside), null,
                        theme("date_below_clock")))
                .add(bool("header_white", R.string.xs_header_white, R.string.sd_header_white, c.headerWhite))
                .add(bool("clock_glow", R.string.xs_clock_glow, R.string.sd_clock_glow, c.clockGlow))
                .add(bool("show_network_pill", R.string.xs_network_pill, R.string.sd_network_pill, c.showNetworkPill))
                .add(bool("wifi_status_color", R.string.xs_wifi_colour, R.string.sd_wifi_colour, c.wifiStatusColor))
                .add(bool("show_inputs_pill", R.string.xs_inputs_pill, R.string.sd_inputs_pill, c.showInputsPill))
                .add(bool("icon_only_buttons", R.string.xs_icon_only_buttons, R.string.sd_icon_only, c.iconOnlyButtons))
                .add(bool("launcher_settings_button", R.string.xs_gear_opens_launcher, R.string.sd_gear,
                        c.launcherSettingsButton));
    }

    // ---- widgets ---------------------------------------------------------------------------------

    private SettingsModel.Category widgetsCommon() {
        return new SettingsModel.Category(R.string.sc_widgets_common, false)
                .add(SettingsModel.Row.action(R.string.xs_widget_places, R.string.sd_widget_places, null, host::widgetPlaces))
                .add(choice("widget_gap", R.string.xs_widget_gap, R.string.sd_widget_gap, c.widgetGap,
                        new Object[]{0, 4, 8, 12, 16, 24, 32}, v -> v + " dp"))
                .add(bool("widget_backdrop", R.string.xs_widget_backdrop, R.string.sd_widget_backdrop, c.widgetBackdrop))
                .add(choice("widget_backdrop_color", R.string.xs_backdrop_color, R.string.sd_backdrop_color,
                        c.widgetBackdropColor, Backdrop.COLORS, v -> a.getResources()
                                .getStringArray(R.array.xs_backdrop_colors)[Arrays.asList(Backdrop.COLORS).indexOf(v)]))
                .add(choice("widget_backdrop_opacity", R.string.xs_backdrop_opacity, R.string.sd_backdrop_opacity,
                        c.widgetBackdropOpacity, new Object[]{0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100}, v -> v + "%"))
                .add(SettingsModel.Row.choice(R.string.xs_backdrop_gradient, R.string.sd_backdrop_gradient,
                        new Object[]{"none", "vertical", "horizontal"}, c.widgetBackdropGradient,
                        v -> text("vertical".equals(v) ? R.string.xs_gradient_vertical
                                : "horizontal".equals(v) ? R.string.xs_gradient_horizontal : R.string.xs_gradient_none),
                        v -> text("vertical".equals(v) ? R.string.sv_gradient_vertical
                                : "horizontal".equals(v) ? R.string.sv_gradient_horizontal : R.string.sv_gradient_none),
                        theme("widget_backdrop_gradient")))
                .add(choice("widget_corner", R.string.xs_widget_corner, R.string.sd_widget_corner, c.widgetCorner,
                        new Object[]{0, 6, 12, 20, 32}, v -> v + " dp"))
                .add(choice("widget_border", R.string.xs_widget_border, R.string.sd_widget_border, c.widgetBorder,
                        new Object[]{0, 1, 2, 3}, v -> (Integer) v == 0 ? text(R.string.off) : v + " dp"))
                .add(choice("widget_border_opacity", R.string.xs_widget_border_opacity, R.string.sd_widget_border_opacity,
                        c.widgetBorderOpacity, new Object[]{10, 20, 30, 50, 70, 100}, v -> v + "%"));
    }

    private SettingsModel.Category newYear() {
        return new SettingsModel.Category(R.string.sc_new_year, false)
                .add(bool("show_new_year", R.string.xs_new_year_on, R.string.sd_new_year_on, c.showNewYear))
                .add(SettingsModel.Row.choice(R.string.xs_new_year_style, R.string.sd_new_year_style,
                        NewYear.STYLES, c.newYearStyle, v -> text(styleName((String) v)), v -> text(styleText((String) v)),
                        theme("new_year_style")))
                .add(SettingsModel.Row.choice(R.string.xs_new_year_target, R.string.sd_new_year_target,
                        NewYear.TARGETS, c.newYearTarget, v -> text("christmas".equals(v) ? R.string.xs_target_christmas
                                : "orthodox_christmas".equals(v) ? R.string.xs_target_orthodox : R.string.xs_target_new_year),
                        null, theme("new_year_target")))
                .add(choice("new_year_text", R.string.xs_new_year_text, R.string.sd_new_year_text, c.newYearText,
                        new Object[]{"short", "long", "soon"}, v -> text("long".equals(v) ? R.string.new_year_title_long
                                : "soon".equals(v) ? R.string.new_year_title_soon : R.string.new_year_until)))
                .add(SettingsModel.Row.action(R.string.xs_celebration_preview, R.string.sd_celebration_preview, null,
                        host::preview));
    }

    /** Network speed add-on, only when it is installed (its provider answers). */
    private SettingsModel.Category netSpeed() {
        android.os.Bundle state = NetSpeedLink.get(a);
        if (state == null) return null;
        SettingsModel.Category cat = new SettingsModel.Category(R.string.sc_net_speed, false)
                .add(SettingsModel.Row.choice(R.string.xs_net_speed_on, R.string.sd_net_speed_on,
                        new Object[]{true, false}, state.getBoolean("on"), this::onOff, null, addOn("on")))
                .add(SettingsModel.Row.choice(R.string.xs_net_speed_every, R.string.sd_net_speed_every,
                        new Object[]{100, 200, 500, 1000, 2000, 5000, 10_000}, state.getInt("interval_ms"),
                        v -> a.getString(R.string.xs_seconds_decimal, (Integer) v / 1000f), null, addOn("interval_ms")))
                .add(SettingsModel.Row.choice(R.string.xs_net_speed_units, R.string.sd_net_speed_units,
                        new Object[]{false, true}, state.getBoolean("bits"),
                        v -> text((Boolean) v ? R.string.xs_units_bits : R.string.xs_units_bytes),
                        v -> text((Boolean) v ? R.string.sv_units_bits : R.string.sv_units_bytes), addOn("bits")));
        return cat;
    }

    private SettingsModel.Category system() {
        SettingsModel.Category cat = new SettingsModel.Category(R.string.sc_system, false);
        if (host.openOnStartAvailable()) {
            cat.add(SettingsModel.Row.action(R.string.open_on_start_label, R.string.sd_open_on_start,
                    host::openOnStartLabel, host::openOnStart));
        }
        cat.add(SettingsModel.Row.action(R.string.home_settings, R.string.sd_default_home, null, host::defaultHome))
                .add(SettingsModel.Row.action(R.string.tv_settings, R.string.sd_tv_settings, null, host::tvSettings))
                .add(SettingsModel.Row.action(R.string.follow_theme, R.string.sd_follow_theme, null, host::followTheme));
        return cat;
    }

    // ---- savers and labels -------------------------------------------------------------------------

    private SettingsModel.Save theme(String key) {
        return v -> {
            Config.putTvOverride(a, key, v);
            host.changed();
        };
    }

    private SettingsModel.Save addOn(String key) {
        return v -> NetSpeedLink.set(a, key, v);
    }

    private SettingsModel.Row choice(String key, int title, int description, Object current, Object[] values,
                                     SettingsModel.Label label) {
        return SettingsModel.Row.choice(title, description, values, current, label, null, theme(key));
    }

    private SettingsModel.Row bool(String key, int title, int description, boolean current) {
        return SettingsModel.Row.choice(title, description, new Object[]{true, false}, current, this::onOff, null,
                theme(key));
    }

    /** One of the launcher's own preference keys (columns, rows below the dock, dock opacity). */
    private SettingsModel.Row prefInt(String key, int title, int description, int current, Object[] values,
                                      SettingsModel.Label label) {
        return SettingsModel.Row.choice(title, description, values, current, label, null, v -> {
            prefs.edit().putInt(key, (Integer) v).apply();
            host.changed();
        });
    }

    private void savePreset(Object value) {
        org.json.JSONObject now = Config.tvOverrides(prefs);
        try {
            if ("claude".equals(value) && !ClaudeWallpaper.ensure(a)) return;
            Config.putTvOverrides(a, "".equals(value) ? Preset.restore(now) : Preset.apply(now, (String) value));
            host.changed();
        } catch (org.json.JSONException ignored) {
            // the choices stay as they were
        }
    }

    private String wallpaperChoice() {
        String w = prefs.getString(Config.WALLPAPER_CHOICE_KEY, null);
        if (w == null) return text(R.string.wallpaper_theme_default);
        return WallpaperRotation.CHOICE_NONE.equals(w) ? text(R.string.wallpaper_none) : w;
    }

    private int installedCards() {
        return a.getPackageManager().queryIntentActivities(new android.content.Intent(android.content.Intent.ACTION_MAIN)
                .addCategory("com.gathin.tvhome.category.DOCK_CARD"), 0).size();
    }

    private String text(int id) {
        return a.getString(id);
    }

    private String onOff(Object v) {
        return text((Boolean) v ? R.string.on : R.string.off);
    }

    private String minutes(Object v) {
        int m = (Integer) v;
        if (m == 0) return text(R.string.off);
        if (m % 60 == 0) return a.getString(R.string.xs_hours, m / 60);
        return a.getString(R.string.xs_minutes, m);
    }

    private String seconds(Object v) {
        return a.getString(R.string.xs_seconds, (Integer) v);
    }

    private static int styleName(String style) {
        switch (style) {
            case "star": return R.string.xs_style_star;
            case "pavuk": return R.string.xs_style_pavuk;
            case "didukh": return R.string.xs_style_didukh;
            case "snow": return R.string.xs_style_snow;
            default: return R.string.xs_style_tree;
        }
    }

    private static int styleText(String style) {
        switch (style) {
            case "star": return R.string.sv_style_star;
            case "pavuk": return R.string.sv_style_pavuk;
            case "didukh": return R.string.sv_style_didukh;
            case "snow": return R.string.sv_style_snow;
            default: return R.string.sv_style_tree;
        }
    }
}
