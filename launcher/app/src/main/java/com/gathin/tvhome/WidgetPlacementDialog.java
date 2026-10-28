package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Launcher settings, Widget places: one row per header widget. OK picks a widget up; then Up and Down move it in the
 * order, Left and Right put it under the clock or under the buttons, and OK or Back sets it down. Saved at once.
 */
final class WidgetPlacementDialog {
    private final Activity activity;
    private final Runnable changed;
    private final List<WidgetLayout.Place> places;
    private final LinearLayout rows;
    private int carried = -1;

    WidgetPlacementDialog(Activity activity, String saved, Runnable changed) {
        this.activity = activity;
        this.changed = changed;
        this.places = WidgetLayout.parse(saved);
        this.rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        rows.setPadding(pad, pad, pad, pad);
    }

    void show() {
        for (int i = 0; i < places.size(); i++) {
            TextView row = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_settings_option, rows, false);
            final int index = i;
            row.setOnClickListener(v -> {
                carried = carried == index ? -1 : index;
                refresh();
            });
            row.setOnKeyListener((v, code, event) -> onKey(index, code, event));
            rows.addView(row);
        }
        refresh();
        new AlertDialog.Builder(activity)
                .setTitle(R.string.xs_widget_places)
                .setMessage(R.string.xs_widget_places_hint)
                .setView(rows)
                .setOnKeyListener((dialog, code, event) -> {
                    if (code == KeyEvent.KEYCODE_BACK && carried >= 0) {
                        if (event.getAction() == KeyEvent.ACTION_UP) {
                            carried = -1;
                            refresh();
                        }
                        return true;   // the first Back sets the widget down; the next one closes
                    }
                    return false;
                })
                .show();
        rows.getChildAt(0).requestFocus();
    }

    private boolean onKey(int index, int code, KeyEvent event) {
        if (carried != index) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN
                    || code == KeyEvent.KEYCODE_DPAD_LEFT || code == KeyEvent.KEYCODE_DPAD_RIGHT;
        }
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                carried = WidgetLayout.move(places, index, code == KeyEvent.KEYCODE_DPAD_UP ? -1 : 1);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                boolean wantRight = code == KeyEvent.KEYCODE_DPAD_RIGHT;
                if (places.get(index).right != wantRight) WidgetLayout.flip(places, index);
                break;
            default:
                return false;
        }
        save();
        refresh();
        rows.getChildAt(carried).requestFocus();
        return true;
    }

    private void save() {
        Config.putTvOverride(activity, "widget_layout", WidgetLayout.format(places));
        changed.run();
    }

    private void refresh() {
        for (int i = 0; i < places.size(); i++) {
            WidgetLayout.Place p = places.get(i);
            String side = activity.getString(p.right ? R.string.xs_widget_right : R.string.xs_widget_left);
            String text = (i + 1) + ". " + activity.getString(title(p.widget)) + " — " + side;
            ((TextView) rows.getChildAt(i)).setText(carried == i ? "↕  " + text + "  ↔" : text);
            rows.getChildAt(i).setActivated(carried == i);
        }
    }

    private static int title(String widget) {
        switch (widget) {
            case "astro": return R.string.xs_astro_short;
            case "stats": return R.string.xs_stats;
            default: return R.string.xs_new_year;
        }
    }
}
