package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.List;

/**
 * Launcher settings as data: categories, each with rows. A row is either a choice among values (OK and the arrows
 * step through them) or an action (OK runs it). Every row carries a description, and a choice can describe each
 * of its values, so the info pane says what the setting does now and what the other values would do.
 */
final class SettingsModel {
    private SettingsModel() { }

    interface Label {
        String of(Object value);
    }

    interface Save {
        void save(Object value);
    }

    interface Text {
        String get();
    }

    static final class Row {
        final int title, description;
        final Object[] values;
        Object current;
        final Label label;
        /** Per-value description, or null when the row's description covers all values. */
        final Label valueDescription;
        final Save save;
        final Runnable action;
        /** An action's current value ("Theme default", an app's name), or null for none. */
        final Text actionValue;

        private Row(int title, int description, Object[] values, Object current, Label label, Label valueDescription,
                    Save save, Runnable action, Text actionValue) {
            this.title = title;
            this.description = description;
            this.values = values;
            this.current = current;
            this.label = label;
            this.valueDescription = valueDescription;
            this.save = save;
            this.action = action;
            this.actionValue = actionValue;
        }

        static Row choice(int title, int description, Object[] values, Object current, Label label,
                          Label valueDescription, Save save) {
            return new Row(title, description, values, current, label, valueDescription, save, null, null);
        }

        static Row action(int title, int description, Text value, Runnable action) {
            return new Row(title, description, null, null, null, null, null, action, value);
        }

        boolean isAction() {
            return action != null;
        }

        String valueText() {
            if (isAction()) return actionValue == null ? null : actionValue.get();
            return label.of(current);
        }

        /** Moves to the next ({@code by} 1) or previous (-1) value and saves it. */
        void step(int by) {
            current = values[SettingsModel.step(values, indexOf(values, current), by)];
            save.save(current);
        }
    }

    static final class Category {
        final int title;
        /** A heading in the list of categories: not a category, only groups the ones under it. */
        final boolean heading;
        final List<Row> rows = new ArrayList<>();

        Category(int title, boolean heading) {
            this.title = title;
            this.heading = heading;
        }

        Category add(Row row) {
            rows.add(row);
            return this;
        }
    }

    static int indexOf(Object[] values, Object current) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(current)) return i;
        return -1;
    }

    /** The index after {@code at} by {@code by}, wrapping round; a value not in the list starts at the first. */
    static int step(Object[] values, int at, int by) {
        if (at < 0) return 0;
        return ((at + by) % values.length + values.length) % values.length;
    }
}
