package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BackdropTest {
    @Test
    public void theColourTakesTheChosenOpacity() {
        assertEquals(0x80000000, Backdrop.argb("#000000", 50));
        assertEquals(0xFFD97757, Backdrop.argb("#D97757", 100));
        assertEquals(0x00FFFFFF, Backdrop.argb("#FFFFFF", 0));
        assertEquals(0x1A0B1026, Backdrop.argb("#0B1026", 10));
    }

    @Test
    public void aBrokenColourFallsBackToBlack() {
        assertEquals(0x80000000, Backdrop.argb("nope", 50));
        assertEquals(0x80000000, Backdrop.argb(null, 50));
    }
}
