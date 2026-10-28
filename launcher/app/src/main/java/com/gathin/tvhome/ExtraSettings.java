package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        action(R.string.xs_wallpaper_gallery, this::showGallery);
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

    // ---- wallpaper gallery -----------------------------------------------------------------------

    /** Every picture of the theme as a thumbnail; OK puts that one on Home (the same choice as the Wallpaper row). */
    private void showGallery() {
        File dir = Config.dir(activity);
        List<File> images = new ArrayList<>(WallpaperRotation.images(dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        if (images.isEmpty()) {
            Toast.makeText(activity, R.string.xs_no_wallpapers, Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences rotation = activity.getSharedPreferences("wallpaper_rotation", Activity.MODE_PRIVATE);
        String showing = rotation.getString("image", "");
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(0xFF101419);
        page.setPadding(dp(56), dp(30), dp(56), dp(10));
        TextView title = new TextView(activity);
        title.setText(activity.getString(R.string.xs_gallery_title, images.size()));
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(28);
        page.addView(title);
        TextView name = new TextView(activity);
        name.setTextColor(0xFFACB8C1);
        name.setTextSize(15);
        name.setPadding(0, dp(4), 0, dp(12));
        page.addView(name);
        RecyclerView list = new RecyclerView(activity);
        list.setLayoutManager(new GridLayoutManager(activity, 4));
        list.setClipToPadding(false);
        list.setPadding(0, dp(6), 0, dp(30));
        ExecutorService decoder = Executors.newSingleThreadExecutor();
        int thumbW = (activity.getResources().getDisplayMetrics().widthPixels - dp(112)) / 4 - dp(16);
        int thumbH = thumbW * 9 / 16;
        list.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
                FrameLayout cell = new FrameLayout(activity);
                cell.setFocusable(true);
                cell.setClickable(true);
                cell.setPadding(dp(4), dp(4), dp(4), dp(4));
                cell.setBackgroundResource(R.drawable.settings_choice_bg);
                ImageView image = new ImageView(activity);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                cell.addView(image, new FrameLayout.LayoutParams(thumbW, thumbH));
                TextView mark = new TextView(activity);
                mark.setText(R.string.xs_gallery_now);
                mark.setTextColor(0xFF101419);
                mark.setTextSize(12);
                mark.setPadding(dp(8), dp(2), dp(8), dp(2));
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(0xFFFFFFFF);
                pill.setCornerRadius(dp(10));
                mark.setBackground(pill);
                FrameLayout.LayoutParams markLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.START);
                markLp.setMargins(dp(8), dp(8), 0, 0);
                cell.addView(mark, markLp);
                RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(dp(8), dp(8), dp(8), dp(8));
                cell.setLayoutParams(lp);
                return new RecyclerView.ViewHolder(cell) { };
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                File file = images.get(position);
                FrameLayout cell = (FrameLayout) holder.itemView;
                ImageView image = (ImageView) cell.getChildAt(0);
                cell.getChildAt(1).setVisibility(file.getName().equals(showing) ? View.VISIBLE : View.GONE);
                image.setImageDrawable(null);
                image.setTag(file);
                decoder.execute(() -> {
                    Bitmap bitmap = thumbnail(file, thumbW);
                    image.post(() -> {
                        if (image.getTag() == file) image.setImageBitmap(bitmap);
                    });
                });
                cell.setOnFocusChangeListener((v, focused) -> {
                    if (focused) name.setText((position + 1) + " / " + images.size() + "   " + file.getName());
                });
                cell.setOnClickListener(v -> {
                    activity.getSharedPreferences(Config.SETTINGS_NAME, Activity.MODE_PRIVATE).edit()
                            .putString(Config.WALLPAPER_CHOICE_KEY, file.getName()).apply();
                    changed.run();
                    Toast.makeText(activity, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                });
            }

            @Override
            public int getItemCount() {
                return images.size();
            }
        });
        page.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        dialog.setContentView(page);
        dialog.setOnDismissListener(d -> decoder.shutdownNow());
        dialog.show();
        list.post(() -> {
            int at = 0;
            for (int i = 0; i < images.size(); i++) if (images.get(i).getName().equals(showing)) at = i;
            int focus = at;
            list.scrollToPosition(focus);
            list.post(() -> {
                RecyclerView.ViewHolder h = list.findViewHolderForAdapterPosition(focus);
                if (h != null) h.itemView.requestFocus();
            });
        });
    }

    /** A small copy of the picture, about {@code width} pixels wide; null when it cannot be read. */
    private static Bitmap thumbnail(File file, int width) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getPath(), bounds);
        if (bounds.outWidth <= 0) return null;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (bounds.outWidth / (opts.inSampleSize * 2) >= width) opts.inSampleSize *= 2;
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(file.getPath(), opts);
    }
}
