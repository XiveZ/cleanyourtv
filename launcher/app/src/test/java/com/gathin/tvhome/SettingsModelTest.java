package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SettingsModelTest {
    private static final Object[] V = {"always", "autohide", "drawer"};

    @Test
    public void stepsGoRoundTheValuesBothWays() {
        assertEquals(1, SettingsModel.step(V, 0, 1));
        assertEquals(0, SettingsModel.step(V, 2, 1));
        assertEquals(2, SettingsModel.step(V, 0, -1));
    }

    @Test
    public void aValueNotInTheListStartsAtTheFirst() {
        assertEquals(0, SettingsModel.step(V, -1, 1));
        assertEquals(0, SettingsModel.step(V, -1, -1));
    }

    @Test
    public void theCurrentValueIsFoundByEquality() {
        assertEquals(1, SettingsModel.indexOf(V, "autohide"));
        assertEquals(-1, SettingsModel.indexOf(V, "nope"));
        assertEquals(1, SettingsModel.indexOf(new Object[]{4, 5, 6}, 5));
    }
}
