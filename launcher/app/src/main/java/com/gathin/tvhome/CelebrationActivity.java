package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

/** Launcher settings, Preview: the five-minute celebration over Home's wallpaper. Left and Right skip 30 seconds. */
public final class CelebrationActivity extends Activity {
    private CelebrationView show;

    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Config cfg = Config.load(this);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(cfg.backgroundColor);
        if (!cfg.wallpapers.isEmpty()) {
            ImageView wall = new ImageView(this);
            wall.setScaleType(ImageView.ScaleType.CENTER_CROP);
            wall.setImageBitmap(decode(cfg.wallpapers.get(0)));
            root.addView(wall, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
        }
        boolean xmas = !"new_year".equals(cfg.newYearTarget);
        show = new CelebrationView(this, getString(xmas ? R.string.new_year_happy_xmas : R.string.new_year_happy), 0,
                this::finish);
        root.addView(show, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        TextView hint = new TextView(this);
        hint.setText(R.string.celebration_preview_hint);
        hint.setTextColor(0xB3FFFFFF);
        hint.setTextSize(14);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        lp.bottomMargin = Math.round(70 * getResources().getDisplayMetrics().density);
        root.addView(hint, lp);
        hint.postDelayed(() -> hint.animate().alpha(0f).setDuration(600).start(), 6000);
        setContentView(root);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_DPAD_RIGHT || code == KeyEvent.KEYCODE_DPAD_LEFT) {
            show.skip(code == KeyEvent.KEYCODE_DPAD_RIGHT ? 30 : -30);
            return true;
        }
        return super.onKeyDown(code, event);
    }

    private Bitmap decode(File f) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int sample = 1;
        while (o.outWidth / (sample * 2) >= 1920) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        return BitmapFactory.decodeFile(f.getPath(), opts);
    }
}
