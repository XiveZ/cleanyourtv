package com.gathin.tvhome;

import android.app.Activity;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws Launcher settings from SettingsModel in three panes: the categories on the left, the chosen category's rows
 * in the middle, and on the right what the focused row does, its value now, and what each other value would do.
 *
 * Keys: in the categories, Up and Down choose (the middle follows), Right or OK go into the rows. In the rows, OK
 * and Right step to the next value (or run an action), Left to the one before; Back returns to the categories.
 */
final class SettingsScreen {
    private final Activity activity;
    private final List<SettingsModel.Category> categories;
    private final LinearLayout rail, content;
    private final ScrollView contentScroll;
    private final TextView infoTitle, infoValue, infoText, infoHint;
    private final List<TextView> railItems = new ArrayList<>();
    private final List<TextView> rowViews = new ArrayList<>();
    private SettingsModel.Category shown;

    SettingsScreen(Activity activity, List<SettingsModel.Category> categories) {
        this.activity = activity;
        this.categories = categories;
        rail = activity.findViewById(R.id.rail);
        content = activity.findViewById(R.id.content);
        contentScroll = activity.findViewById(R.id.contentScroll);
        infoTitle = activity.findViewById(R.id.infoTitle);
        infoValue = activity.findViewById(R.id.infoValue);
        infoText = activity.findViewById(R.id.infoText);
        infoHint = activity.findViewById(R.id.infoHint);
    }

    void show() {
        TextView title = text(activity.getString(R.string.launcher_settings), 24, 0xFFFFFFFF);
        title.setPadding(dp(6), 0, 0, dp(12));
        rail.addView(title);
        for (SettingsModel.Category c : categories) {
            if (c.heading) {
                TextView h = text(activity.getString(c.title).toUpperCase(java.util.Locale.ROOT), 12, 0xFF8A96A0);
                h.setLetterSpacing(0.1f);
                h.setPadding(dp(8), dp(14), 0, dp(4));
                rail.addView(h);
                continue;
            }
            TextView item = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_settings_option, rail, false);
            ((LinearLayout.LayoutParams) item.getLayoutParams()).topMargin = dp(4);
            item.setText(c.title);
            item.setTextSize(16);
            item.setOnFocusChangeListener((v, focused) -> {
                if (focused) select(c);
            });
            item.setOnClickListener(v -> enterRows());
            item.setOnKeyListener((v, code, event) -> {
                if (code != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
                if (event.getAction() == KeyEvent.ACTION_DOWN) enterRows();
                return true;
            });
            rail.addView(item);
            railItems.add(item);
        }
        if (!railItems.isEmpty()) railItems.get(0).requestFocus();
    }

    /** Back: from the rows to the categories (true); from the categories, the caller closes the screen. */
    boolean back() {
        if (!content.hasFocus()) return false;
        int at = indexOfShown();
        if (at >= 0) railItems.get(at).requestFocus();
        categoryInfo(shown);
        return true;
    }

    /** The values may have changed elsewhere (a picker, a dialog): show them again. */
    void refresh() {
        if (shown == null) return;
        for (int i = 0; i < rowViews.size(); i++) label(rowViews.get(i), shown.rows.get(i));
        View f = activity.getCurrentFocus();
        int at = rowViews.indexOf(f);
        if (at >= 0) info(shown.rows.get(at));
    }

    private int indexOfShown() {
        int k = 0;
        for (SettingsModel.Category c : categories) {
            if (c.heading) continue;
            if (c == shown) return k;
            k++;
        }
        return -1;
    }

    private void select(SettingsModel.Category c) {
        if (c == shown) return;
        shown = c;
        for (int i = 0; i < railItems.size(); i++) railItems.get(i).setSelected(i == indexOfShown());
        content.removeAllViews();
        rowViews.clear();
        TextView heading = text(activity.getString(c.title), 26, 0xFFFFFFFF);
        heading.setPadding(dp(4), 0, 0, dp(6));
        content.addView(heading);
        for (SettingsModel.Row r : c.rows) {
            TextView row = (TextView) LayoutInflater.from(activity).inflate(R.layout.item_settings_option, content, false);
            // Long names wrap onto a second line instead of being cut by the fixed row height.
            row.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
            row.setMinHeight(dp(52));
            row.setPadding(row.getPaddingLeft(), dp(10), row.getPaddingRight(), dp(10));
            row.setTextSize(17);
            label(row, r);
            row.setOnFocusChangeListener((v, focused) -> {
                if (focused) info(r);
            });
            row.setOnClickListener(v -> {
                if (r.isAction()) r.action.run();
                else r.step(1);
                label(row, r);
                info(r);
            });
            row.setOnKeyListener((v, code, event) -> onRowKey(r, row, code, event));
            content.addView(row);
            rowViews.add(row);
        }
        contentScroll.scrollTo(0, 0);
        categoryInfo(c);
    }

    private void categoryInfo(SettingsModel.Category c) {
        infoTitle.setText(c.title);
        infoValue.setText("");
        infoText.setText(R.string.ss_category_hint);
        infoHint.setText(R.string.ss_keys_rail);
    }

    private boolean onRowKey(SettingsModel.Row r, TextView row, int code, KeyEvent event) {
        boolean left = code == KeyEvent.KEYCODE_DPAD_LEFT, right = code == KeyEvent.KEYCODE_DPAD_RIGHT;
        if (!left && !right) return false;
        if (event.getAction() != KeyEvent.ACTION_DOWN) return true;
        if (r.isAction()) {
            if (left) back();
            else r.action.run();
            return true;
        }
        r.step(right ? 1 : -1);
        label(row, r);
        info(r);
        return true;
    }

    private void enterRows() {
        if (!rowViews.isEmpty()) rowViews.get(0).requestFocus();
    }

    private void label(TextView row, SettingsModel.Row r) {
        String value = r.valueText();
        String title = activity.getString(r.title);
        if (r.isAction()) row.setText(value == null ? title + "   ›" : title + ":   " + value + "   ›");
        // The value and its arrows wrap as one piece, never "‹ On" on one line and "›" on the next.
        else row.setText(title + ":   " + ("‹ " + value + " ›").replace(' ', '\u00A0'));
    }

    /** The right pane: what the row does, its value now, and every value's effect with the current one marked. */
    private void info(SettingsModel.Row r) {
        infoTitle.setText(r.title);
        String value = r.valueText();
        infoValue.setText(value == null ? "" : activity.getString(R.string.ss_now, value));
        SpannableStringBuilder sb = new SpannableStringBuilder(activity.getString(r.description));
        if (r.valueDescription != null) {
            sb.append("\n\n");
            int head = sb.length();
            sb.append(activity.getString(R.string.ss_values));
            sb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), head, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            for (Object v : r.values) {
                boolean now = v.equals(r.current);
                sb.append("\n");
                int start = sb.length();
                sb.append(now ? "▸ " : "• ").append(r.label.of(v)).append(" — ").append(r.valueDescription.of(v));
                if (now) {
                    sb.setSpan(new ForegroundColorSpan(0xFFB8E0C8), start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        infoText.setText(sb);
        infoHint.setText(r.isAction() ? R.string.ss_keys_action : R.string.ss_keys_choice);
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private int dp(float v) {
        return Math.round(v * activity.getResources().getDisplayMetrics().density);
    }
}
