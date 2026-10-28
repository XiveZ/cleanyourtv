package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.Typeface;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;

/**
 * The Claude preset's wallpaper, drawn here rather than downloaded: warm charcoal, a soft clay light, a field of
 * points joined where they are close (thoughts finding each other), a spark of uneven rays, and a line that says
 * when and with whom this launcher was made. 1920 x 1080, written once to files/presets/.
 */
final class ClaudeWallpaper {
    private ClaudeWallpaper() { }

    /** The name in files/presets/; a new drawing gets a new name so an old file is never shown. */
    static final String FILE_NAME = "claude-2.jpg";
    private static final int CLAY = 0xFFD97757, CREAM = 0xFFF0EEE6, CHARCOAL = 0xFF141413;

    static File file(Context ctx) {
        return new File(new File(ctx.getFilesDir(), "presets"), FILE_NAME);
    }

    /** Draws and saves the picture if it is not there yet. False when it could not be written. */
    static boolean ensure(Context ctx) {
        File f = file(ctx);
        if (f.isFile() && f.length() > 0) return true;
        File dir = f.getParentFile();
        if (dir == null || (!dir.isDirectory() && !dir.mkdirs())) return false;
        File[] old = dir.listFiles((d, name) -> name.startsWith("claude-") && !name.equals(FILE_NAME));
        if (old != null) for (File o : old) o.delete();   // an earlier drawing
        Bitmap b = draw(1920, 1080);
        try (FileOutputStream out = new FileOutputStream(f)) {
            return b.compress(Bitmap.CompressFormat.JPEG, 92, out);
        } catch (java.io.IOException e) {
            return false;
        } finally {
            b.recycle();
        }
    }

    static Bitmap draw(int w, int h) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF1C1B18, CHARCOAL, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        float cx = w * 0.70f, cy = h * 0.53f;   // clear of the widgets above and the dock below
        p.setShader(new RadialGradient(cx, cy, h * 0.62f, 0x47D97757, 0x00D97757, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        // Thin rings around the light, like ripples.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.5f);
        for (int i = 1; i <= 4; i++) {
            p.setColor(withAlpha(CLAY, 34 - i * 6));
            c.drawCircle(cx, cy, 120 + i * 85, p);
        }

        // Points joined to their near neighbours.
        Random r = new Random(20261028);
        int n = 90;
        float[] x = new float[n], y = new float[n];
        for (int i = 0; i < n; i++) {
            x[i] = w * (0.38f + 0.62f * r.nextFloat());
            y[i] = h * (0.06f + 0.70f * r.nextFloat());
        }
        p.setStrokeWidth(1.2f);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                float dist = (float) Math.hypot(x[i] - x[j], y[i] - y[j]);
                if (dist > 150) continue;
                p.setColor(withAlpha(CREAM, Math.round(40 * (1 - dist / 150))));
                c.drawLine(x[i], y[i], x[j], y[j], p);
            }
        }
        p.setStyle(Paint.Style.FILL);
        for (int i = 0; i < n; i++) {
            p.setColor(withAlpha(i % 7 == 0 ? CLAY : CREAM, i % 7 == 0 ? 200 : 110));
            c.drawCircle(x[i], y[i], i % 7 == 0 ? 3.5f : 2f, p);
        }

        // The spark: uneven rays, rounded, warm at the root and pale at the tip.
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        int rays = 11;
        for (int i = 0; i < rays; i++) {
            double a = Math.toRadians(-90 + 360.0 / rays * i + (r.nextFloat() - 0.5f) * 9);
            float len = 50 + r.nextFloat() * 45;
            float ex = cx + (float) Math.cos(a) * len, ey = cy + (float) Math.sin(a) * len;
            p.setStrokeWidth(10 + r.nextFloat() * 4);
            p.setShader(new LinearGradient(cx, cy, ex, ey, CLAY, 0xFFF2B89B, Shader.TileMode.CLAMP));
            c.drawLine(cx + (float) Math.cos(a) * 14, cy + (float) Math.sin(a) * 14, ex, ey, p);
        }
        p.setShader(null);
        p.setStyle(Paint.Style.FILL);
        p.setColor(CLAY);
        c.drawCircle(cx, cy, 16, p);

        // The signature, quiet, under the spark.
        p.setTypeface(Typeface.MONOSPACE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(30);
        p.setColor(withAlpha(CREAM, 150));
        c.drawText("› claude code", cx, cy + 150, p);
        p.setTextSize(22);
        p.setColor(withAlpha(CREAM, 95));
        c.drawText("Разам з Claude Code · 28.10.2026", cx, cy + 184, p);
        return b;
    }

    private static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
