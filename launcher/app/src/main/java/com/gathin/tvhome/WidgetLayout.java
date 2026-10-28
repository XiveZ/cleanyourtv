package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where the header widgets go (widget_layout): an order, and for each the left column under the clock or the right
 * one under the top buttons. Saved as "astro:left,stats:left,newyear:right"; every known widget appears once.
 */
final class WidgetLayout {
    private WidgetLayout() { }

    static final List<String> ALL = Collections.unmodifiableList(Arrays.asList("astro", "stats", "newyear"));

    static final class Place {
        final String widget;
        boolean right;

        Place(String widget, boolean right) {
            this.widget = widget;
            this.right = right;
        }
    }

    static List<Place> parse(String saved) {
        List<Place> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        if (saved != null) {
            for (String part : saved.split(",")) {
                String[] kv = part.trim().split(":");
                if (!ALL.contains(kv[0]) || seen.contains(kv[0])) continue;
                seen.add(kv[0]);
                out.add(new Place(kv[0], kv.length > 1 && "right".equals(kv[1])));
            }
        }
        for (String w : ALL) if (!seen.contains(w)) out.add(new Place(w, false));
        return out;
    }

    static String format(List<Place> places) {
        StringBuilder sb = new StringBuilder();
        for (Place p : places) {
            if (sb.length() > 0) sb.append(',');
            sb.append(p.widget).append(p.right ? ":right" : ":left");
        }
        return sb.toString();
    }

    /** Swaps the widget at {@code at} with its neighbour {@code by} (-1 up, +1 down); nothing at either end. */
    static int move(List<Place> places, int at, int by) {
        int to = at + by;
        if (to < 0 || to >= places.size()) return at;
        Collections.swap(places, at, to);
        return to;
    }

    static void flip(List<Place> places, int at) {
        places.get(at).right = !places.get(at).right;
    }
}
