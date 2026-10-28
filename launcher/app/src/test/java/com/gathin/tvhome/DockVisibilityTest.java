package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import android.view.KeyEvent;

import org.junit.Test;

public class DockVisibilityTest {
    private static DockVisibility.KeyAction hidden(boolean inGrid, int code) {
        return DockVisibility.decide(true, inGrid, code, false);
    }

    @Test
    public void aHiddenDockComesBackWithDownOrOkAndUpGoesToTheHeader() {
        assertEquals(DockVisibility.KeyAction.REVEAL, hidden(true, KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals(DockVisibility.KeyAction.REVEAL, hidden(true, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(DockVisibility.KeyAction.REVEAL, hidden(true, KeyEvent.KEYCODE_DPAD_LEFT));
        assertEquals(DockVisibility.KeyAction.TO_HEADER, hidden(true, KeyEvent.KEYCODE_DPAD_UP));
    }

    @Test
    public void inTheHeaderOnlyDownBringsTheDockAndOtherKeysWorkAsUsual() {
        assertEquals(DockVisibility.KeyAction.REVEAL, hidden(false, KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals(DockVisibility.KeyAction.PASS, hidden(false, KeyEvent.KEYCODE_DPAD_LEFT));
        assertEquals(DockVisibility.KeyAction.PASS, hidden(false, KeyEvent.KEYCODE_DPAD_CENTER));
        assertEquals(DockVisibility.KeyAction.PASS, hidden(true, KeyEvent.KEYCODE_VOLUME_UP));
    }

    @Test
    public void backPutsAShownDockAwayUnlessAMenuIsOpen() {
        assertEquals(DockVisibility.KeyAction.HIDE, DockVisibility.decide(false, true, KeyEvent.KEYCODE_BACK, false));
        assertEquals(DockVisibility.KeyAction.PASS, DockVisibility.decide(false, true, KeyEvent.KEYCODE_BACK, true));
        assertEquals(DockVisibility.KeyAction.PASS, DockVisibility.decide(false, false, KeyEvent.KEYCODE_BACK, false));
        assertEquals(DockVisibility.KeyAction.PASS, DockVisibility.decide(false, true, KeyEvent.KEYCODE_DPAD_DOWN, false));
    }
}
