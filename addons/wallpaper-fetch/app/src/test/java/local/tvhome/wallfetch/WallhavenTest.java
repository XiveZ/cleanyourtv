package local.tvhome.wallfetch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class WallhavenTest {
    @Test
    public void theSearchAsksForSafeSixteenByNineAnimeOfAtLeastFullHd() {
        String url = Wallhaven.searchUrl("night sky", "333399", 2);
        assertTrue(url, url.startsWith("https://wallhaven.cc/api/v1/search?"));
        assertTrue(url, url.contains("categories=010"));
        assertTrue(url, url.contains("purity=100"));
        assertTrue(url, url.contains("ratios=16x9"));
        assertTrue(url, url.contains("atleast=1920x1080"));
        assertTrue(url, url.contains("colors=333399"));
        assertTrue(url, url.contains("page=2"));
        assertTrue(url, url.contains("q=night+sky+-weapon"));
    }

    @Test
    public void everyTagIsSearchedWithThreeColours() {
        assertEquals(Wallhaven.TAGS.length * 3, Wallhaven.pairs().size());
    }

    @Test
    public void theAnswerGivesEachPicturesIdAndFullImage() throws Exception {
        List<Wallhaven.Item> items = Wallhaven.parse("{\"data\":[{\"id\":\"abc\",\"path\":\"https://w.wallhaven.cc/full/ab/wallhaven-abc.jpg\"},"
                + "{\"id\":\"x\"},{\"id\":\"def\",\"path\":\"https://w.wallhaven.cc/full/de/wallhaven-def.png\"}]}");
        assertEquals(2, items.size());
        assertEquals("abc", items.get(0).id);
        assertEquals("https://w.wallhaven.cc/full/de/wallhaven-def.png", items.get(1).url);
    }

    @Test
    public void onlyWallhavenImagesAreDownloaded() throws Exception {
        List<Wallhaven.Item> items = Wallhaven.parse("{\"data\":[{\"id\":\"a\",\"path\":\"http://evil.example/a.jpg\"},"
                + "{\"id\":\"../b\",\"path\":\"https://w.wallhaven.cc/full/b.jpg\"}]}");
        assertEquals(0, items.size());
    }
}
