package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.json.JSONObject;
import org.junit.Test;

public class TvOverridesTest {
    @Test
    public void aChoiceOnTheTvWinsOverTheThemeAndKeepsTheRest() throws Exception {
        JSONObject theme = new JSONObject("{\"wallpaper_dim\":35,\"dock_glass\":true}");
        JSONObject merged = Config.withTvOverrides(theme, "{\"wallpaper_dim\":50,\"dock_mode\":\"drawer\"}");
        assertEquals(50, merged.getInt("wallpaper_dim"));
        assertEquals("drawer", merged.getString("dock_mode"));
        assertEquals(true, merged.getBoolean("dock_glass"));
        // The theme read from config.json is not changed in place.
        assertEquals(35, theme.getInt("wallpaper_dim"));
    }

    @Test
    public void choicesWithoutAThemeStillApply() throws Exception {
        JSONObject merged = Config.withTvOverrides(null, "{\"fill_dock\":false}");
        assertFalse(merged.getBoolean("fill_dock"));
    }

    @Test
    public void noChoicesOrBrokenOnesLeaveTheThemeAsItIs() throws Exception {
        JSONObject theme = new JSONObject("{\"wallpaper_dim\":35}");
        assertSame(theme, Config.withTvOverrides(theme, null));
        assertSame(theme, Config.withTvOverrides(theme, ""));
        assertSame(theme, Config.withTvOverrides(theme, "{not json"));
        assertNull(Config.withTvOverrides(null, null));
    }
}
