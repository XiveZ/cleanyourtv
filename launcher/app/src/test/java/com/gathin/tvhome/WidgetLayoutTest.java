package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class WidgetLayoutTest {
    private static String names(List<WidgetLayout.Place> places) {
        StringBuilder sb = new StringBuilder();
        for (WidgetLayout.Place p : places) sb.append(p.widget).append(p.right ? ">" : "<").append(' ');
        return sb.toString().trim();
    }

    @Test
    public void theSavedLayoutIsReadWithEachWidgetOnceAndUnknownOnesDropped() {
        assertEquals("newyear< astro> stats<", names(WidgetLayout.parse("newyear:left,astro:right,stats:left")));
        assertEquals("stats< astro< newyear<", names(WidgetLayout.parse("stats:left,bogus:right,stats:right")));
    }

    @Test
    public void anEmptyOrBrokenLayoutGivesTheDefaultOrder() {
        assertEquals("astro< stats< newyear<", names(WidgetLayout.parse("")));
        assertEquals("astro< stats< newyear<", names(WidgetLayout.parse(null)));
    }

    @Test
    public void movingUpAndDownSwapsWithTheNeighbourAndSidesFlip() {
        List<WidgetLayout.Place> p = WidgetLayout.parse("");
        WidgetLayout.move(p, 1, -1);   // stats up
        assertEquals("stats< astro< newyear<", names(p));
        WidgetLayout.move(p, 0, -1);   // already first: stays
        assertEquals("stats< astro< newyear<", names(p));
        WidgetLayout.flip(p, 2);
        assertEquals("stats< astro< newyear>", names(p));
        assertEquals("stats:left,astro:left,newyear:right", WidgetLayout.format(p));
        assertEquals(Arrays.asList("astro", "stats", "newyear"), WidgetLayout.ALL);
    }
}
