package com.gathin.tvhome;

import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

/**
 * Puts the header widgets in their columns and order (widget_layout), widget_gap dp apart and from the clock, on an
 * optional dark pill each (widget_backdrop) so the text reads on a bright wallpaper.
 */
final class HeaderWidgets {
    private HeaderWidgets() { }

    static void arrange(LinearLayout left, LinearLayout right, Map<String, View> widgets, Config cfg) {
        float d = left.getResources().getDisplayMetrics().density;
        int gap = Math.round(cfg.widgetGap * d);
        for (WidgetLayout.Place place : WidgetLayout.parse(cfg.widgetLayout)) {
            View v = widgets.get(place.widget);
            if (v == null) continue;
            if (v.getParent() instanceof ViewGroup) ((ViewGroup) v.getParent()).removeView(v);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = gap;
            lp.gravity = place.right ? Gravity.END : Gravity.START;
            if (v instanceof TextView) {
                ((TextView) v).setGravity(place.right ? Gravity.RIGHT : Gravity.LEFT);
                ((TextView) v).setLineSpacing(Math.max(2, gap / 3f), 1f);
            }
            if (cfg.widgetBackdrop) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(0x80000000);
                pill.setCornerRadius(12 * d);
                v.setBackground(pill);
                v.setPadding(Math.round(12 * d), Math.round(6 * d), Math.round(12 * d), Math.round(6 * d));
            }
            (place.right ? right : left).addView(v, lp);
        }
    }
}
