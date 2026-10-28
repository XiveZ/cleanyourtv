package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.View;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * Local widget (show_new_year): a small fir tree with twinkling lights and sparkles, "Until the New Year" and the
 * countdown in days and HH:MM:SS, every second. Drawn on one canvas, about 360 x 100 dp; the sparkles run at about
 * 10 frames a second only while Home is on screen, and not at all with animations off (then once a second).
 */
final class NewYearView extends View {
    private static final int SPARKLES = 16;
    private final float d;
    private final boolean animate;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint title = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint timer = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float[] sx = new float[SPARKLES], sy = new float[SPARKLES], phase = new float[SPARKLES];
    private final int[] lights = {0xFFFF5A5A, 0xFFFFD54F, 0xFF64B5F6, 0xFFBA68C8, 0xFFFF8A65, 0xFF81C784};
    private Shader treeShade;

    NewYearView(Context ctx, boolean animate) {
        super(ctx);
        this.animate = animate;
        d = ctx.getResources().getDisplayMetrics().density;
        float sp = ctx.getResources().getDisplayMetrics().scaledDensity;
        title.setColor(0xD9FFFFFF);
        title.setTextSize(16 * sp);
        title.setTypeface(UiFont.get(ctx, R.string.font_medium));
        title.setLetterSpacing(0.06f);
        timer.setColor(Color.WHITE);
        timer.setTextSize(26 * sp);
        timer.setTypeface(Typeface.create(UiFont.get(ctx, R.string.font_medium), Typeface.BOLD));
        timer.setFontFeatureSettings("tnum");   // the digits keep their width every second
        Random r = new Random(2027);
        for (int i = 0; i < SPARKLES; i++) {
            sx[i] = r.nextFloat();
            sy[i] = r.nextFloat();
            phase[i] = r.nextFloat() * 6.283f;
        }
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(Math.round(360 * d), Math.round(100 * d));
    }

    @Override
    protected void onDraw(Canvas c) {
        float t = SystemClock.uptimeMillis() / 1000f;
        drawTree(c, t);
        drawSparkles(c, t);
        long left = NewYear.secondsLeft(LocalDateTime.now());
        float x = 92 * d;
        if (left == 0) {
            c.drawText(getContext().getString(R.string.new_year_happy), x, 60 * d, timer);
        } else {
            NewYear.Left l = NewYear.split(left);
            c.drawText(getContext().getString(R.string.new_year_until), x, 36 * d, title);
            String days = getResources().getQuantityString(R.plurals.new_year_days, (int) l.days, (int) l.days);
            c.drawText(days + "  " + l.clock, x, 74 * d, timer);
        }
        if (!isShown()) return;
        if (animate) {
            postInvalidateDelayed(100);   // 10 frames a second: twinkling, at a fraction of the CPU
        } else {
            postInvalidateDelayed(1000 - System.currentTimeMillis() % 1000);   // on the second
        }
    }

    private void drawTree(Canvas c, float t) {
        float cx = 40 * d, top = 12 * d;
        if (treeShade == null) {
            treeShade = new LinearGradient(0, top, 0, 82 * d, 0xFF43A047, 0xFF1B5E20, Shader.TileMode.CLAMP);
        }
        fill.setShader(treeShade);
        // Three tiers, each wider and lower: {apex, half width, base} in dp.
        float[][] tiers = {{16, 14, 40}, {30, 20, 60}, {46, 27, 82}};
        for (float[] tier : tiers) {
            path.reset();
            path.moveTo(cx, tier[0] * d);
            path.lineTo(cx - tier[1] * d, tier[2] * d);
            path.lineTo(cx + tier[1] * d, tier[2] * d);
            path.close();
            c.drawPath(path, fill);
        }
        fill.setShader(null);
        fill.setColor(0xFF6D4C41);
        c.drawRect(cx - 4 * d, 82 * d, cx + 4 * d, 92 * d, fill);
        // The star on top glows a little.
        float glow = animate ? 0.75f + 0.25f * (float) Math.sin(t * 2.4f) : 1f;
        fill.setColor(Color.argb(Math.round(255 * glow), 255, 213, 79));
        star(c, cx, top + 2 * d, 7 * d, 3 * d);
        // Lights on the tiers blink in turn.
        float[][] at = {{-5, 30}, {6, 36}, {-11, 50}, {4, 54}, {13, 57}, {-19, 76}, {-5, 72}, {10, 78}, {20, 79}};
        for (int i = 0; i < at.length; i++) {
            float on = animate ? 0.35f + 0.65f * Math.max(0f, (float) Math.sin(t * 3f + i * 1.3f)) : 1f;
            int col = lights[i % lights.length];
            fill.setColor(Color.argb(Math.round(255 * on), Color.red(col), Color.green(col), Color.blue(col)));
            c.drawCircle(cx + at[i][0] * d, at[i][1] * d, 2.6f * d, fill);
        }
    }

    private void drawSparkles(Canvas c, float t) {
        float w = getWidth(), h = getHeight();
        for (int i = 0; i < SPARKLES; i++) {
            float a = animate ? Math.max(0f, (float) Math.sin(t * (1.4f + i % 4 * 0.35f) + phase[i])) : 0.5f;
            if (a < 0.05f) continue;
            fill.setColor(Color.argb(Math.round(220 * a), 255, 255, 255));
            star(c, sx[i] * w, 4 * d + sy[i] * (h - 8 * d), (1.5f + 2.5f * a) * d, 0.6f * d);
        }
    }

    /** A four-pointed sparkle. */
    private void star(Canvas c, float x, float y, float outer, float inner) {
        path.reset();
        for (int k = 0; k < 8; k++) {
            double ang = Math.PI / 4 * k - Math.PI / 2;
            float r = k % 2 == 0 ? outer : inner;
            float px = x + (float) Math.cos(ang) * r, py = y + (float) Math.sin(ang) * r;
            if (k == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        c.drawPath(path, fill);
    }
}
