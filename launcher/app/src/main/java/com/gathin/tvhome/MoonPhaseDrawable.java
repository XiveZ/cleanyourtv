package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** The moon as it looks today: the lit share on the side it grows from (right while waxing, as seen in Belarus). */
final class MoonPhaseDrawable extends Drawable {
    private final float lit;
    private final boolean waxing;
    private final Paint dark = new Paint(Paint.ANTI_ALIAS_FLAG), light = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF box = new RectF();

    MoonPhaseDrawable(double lit, boolean waxing) {
        this.lit = (float) Math.max(0, Math.min(1, lit));
        this.waxing = waxing;
        dark.setColor(0xFF3A3D45);   // the unlit side, opaque so the terminator can cover the lit half exactly
        light.setColor(0xFFF5F0DC);
    }

    @Override
    public void draw(Canvas c) {
        float r = Math.min(getBounds().width(), getBounds().height()) / 2f * 0.9f;
        float cx = getBounds().exactCenterX(), cy = getBounds().exactCenterY();
        c.drawCircle(cx, cy, r, dark);
        // The lit half on its side, then the terminator: an ellipse that darkens (crescent) or lights (gibbous).
        box.set(cx - r, cy - r, cx + r, cy + r);
        path.reset();
        path.arcTo(box, waxing ? -90 : 90, 180);
        path.close();
        c.drawPath(path, light);
        float rx = r * Math.abs(1 - 2 * lit);
        box.set(cx - rx, cy - r, cx + rx, cy + r);
        c.drawOval(box, lit < 0.5f ? dark : light);
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
