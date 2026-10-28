package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GlassLiftTest {
    @Test
    public void theHighlightLiftsOnlyWithTilesThatLift() {
        // advanced_effects lifts grid tiles 4dp; dock tiles stay in place (#2135), so their highlight must too.
        assertEquals(8f, MainActivity.glassLiftPx(true, false, 2f), 0.001f);
        assertEquals(0f, MainActivity.glassLiftPx(true, true, 2f), 0.001f);
        assertEquals(0f, MainActivity.glassLiftPx(false, false, 2f), 0.001f);
    }
}
