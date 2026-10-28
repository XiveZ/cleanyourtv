package com.gathin.tvhome;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.text.style.ReplacementSpan;

/** A drawable in a line of text, as tall as the letters and centred on them (ALIGN_CENTER needs API 29). */
final class IconSpan extends ReplacementSpan {
    private final Drawable icon;

    IconSpan(Drawable icon) {
        this.icon = icon;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.getTextSize() * 1.25f);
    }

    @Override
    public void draw(Canvas c, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        int size = Math.round(paint.getTextSize() * 1.25f);
        Paint.FontMetricsInt fm = paint.getFontMetricsInt();
        int centre = y + (fm.ascent + fm.descent) / 2;
        icon.setBounds(Math.round(x), centre - size / 2, Math.round(x) + size, centre + size / 2);
        icon.draw(c);
    }
}
