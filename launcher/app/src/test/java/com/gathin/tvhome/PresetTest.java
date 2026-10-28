package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class PresetTest {
    @Test
    public void applyingSetsThePresetAndRememberingWhatWasThere() throws Exception {
        JSONObject before = new JSONObject("{\"widget_gap\":16,\"dock_mode\":\"drawer\"}");
        JSONObject on = Preset.apply(before, "claude");
        assertEquals("claude", Preset.active(on));
        assertEquals("rim", on.getString("glass_style"));
        assertEquals(12, on.getInt("widget_gap"));
        assertEquals("drawer", on.getString("dock_mode"));   // not part of the preset: kept
    }

    @Test
    public void switchingBackRestoresExactlyThePreviousChoices() throws Exception {
        JSONObject before = new JSONObject("{\"widget_gap\":16,\"dock_mode\":\"drawer\"}");
        JSONObject back = Preset.restore(Preset.apply(before, "claude"));
        assertEquals(before.toString(), new JSONObject(back.toString()).toString());
        assertEquals("", Preset.active(back));
        assertFalse(back.has("glass_style"));   // was not set before: removed again
    }

    @Test
    public void applyingTwiceKeepsTheFirstBackup() throws Exception {
        JSONObject before = new JSONObject("{\"widget_gap\":16}");
        JSONObject twice = Preset.apply(Preset.apply(before, "claude"), "claude");
        assertEquals(16, Preset.restore(twice).getInt("widget_gap"));
    }

    @Test
    public void anUnknownPresetChangesNothing() throws Exception {
        JSONObject before = new JSONObject("{\"widget_gap\":16}");
        assertEquals(before.toString(), Preset.apply(before, "nope").toString());
        assertTrue(Preset.restore(before).has("widget_gap"));
    }
}
