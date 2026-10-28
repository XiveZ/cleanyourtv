package local.tvhome.wallfetch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CoverTest {
    @Test
    public void aWiderPictureIsCutAtTheSidesToFillTheScreen() {
        Cover c = Cover.of(3840, 1600, 1920, 1080);
        assertEquals(1080f / 1600f, c.scale, 1e-6);
        assertEquals(Math.round((3840 * c.scale - 1920) / 2), c.left);
        assertEquals(0, c.top);
    }

    @Test
    public void aTallerPictureIsCutAtTheTopAndBottom() {
        Cover c = Cover.of(1920, 1440, 1920, 1080);
        assertEquals(1f, c.scale, 1e-6);
        assertEquals(0, c.left);
        assertEquals(180, c.top);
    }

    @Test
    public void bigPicturesAreDecodedSmallerButNeverUnderTheScreen() {
        assertEquals(2, Cover.sampleSize(5120, 2880, 1920, 1080));
        assertEquals(1, Cover.sampleSize(2560, 1440, 1920, 1080));
        assertEquals(4, Cover.sampleSize(7680, 4320, 1920, 1080));
    }
}
