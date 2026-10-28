package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class WallpaperImportTest {
    @Test
    public void onlyPlainPictureNamesFromTheAddOnAreTaken() {
        assertTrue(WallpaperImport.safeName("wh-abc123.jpg"));
        assertFalse(WallpaperImport.safeName("../order.txt"));
        assertFalse(WallpaperImport.safeName("a/b.jpg"));
        assertFalse(WallpaperImport.safeName("config.json"));
        assertFalse(WallpaperImport.safeName(""));
    }

    @Test
    public void aNewerSetWithPicturesIsTakenOnce() {
        assertTrue(WallpaperImport.shouldImport(200, 100, new String[]{"wh-a.jpg"}));
        assertFalse(WallpaperImport.shouldImport(100, 100, new String[]{"wh-a.jpg"}));
        assertFalse(WallpaperImport.shouldImport(200, 100, new String[0]));
        assertFalse(WallpaperImport.shouldImport(200, 100, new String[]{"../x.jpg"}));
    }

    @Test
    public void unsafeNamesAreLeftOut() {
        assertEquals(Arrays.asList("wh-a.jpg", "wh-b.jpg"),
                WallpaperImport.safeNames(new String[]{"wh-a.jpg", "../x.jpg", "wh-b.jpg"}));
    }
}
