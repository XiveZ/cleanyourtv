package com.gathin.tvhome;

import android.app.Activity;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
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
 * Launcher settings' Wallpaper row: every picture of the theme as a thumbnail, plus Theme (its own rotation) and
 * None. OK saves the choice (Config.WALLPAPER_CHOICE_KEY) and Home rebuilds with it.
 */
final class WallpaperGallery {
    private final Activity activity;
    private final Runnable changed;

    WallpaperGallery(Activity activity, Runnable changed) {
        this.activity = activity;
        this.changed = changed;
    }

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }

    /** Every picture of the theme as a thumbnail; OK puts that one on Home (the same choice as the Wallpaper row). */
    void show() {
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
        LinearLayout choices = new LinearLayout(activity);
        choices.setOrientation(LinearLayout.HORIZONTAL);
        choices.addView(galleryButton(R.string.wallpaper_theme_default, dialog, null));
        choices.addView(galleryButton(R.string.wallpaper_none, dialog, WallpaperRotation.CHOICE_NONE));
        page.addView(choices);
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

    /** A row above the thumbnails: back to the theme's rotation (null) or no wallpaper. */
    private TextView galleryButton(int title, Dialog dialog, String choice) {
        TextView b = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_settings_option, null, false);
        b.setText(title);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(52), 1f);
        lp.setMargins(dp(8), 0, dp(8), dp(10));
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> {
            SharedPreferences.Editor e = activity.getSharedPreferences(Config.SETTINGS_NAME, Activity.MODE_PRIVATE).edit();
            if (choice == null) e.remove(Config.WALLPAPER_CHOICE_KEY);
            else e.putString(Config.WALLPAPER_CHOICE_KEY, choice);
            e.apply();
            changed.run();
            dialog.dismiss();
        });
        return b;
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
