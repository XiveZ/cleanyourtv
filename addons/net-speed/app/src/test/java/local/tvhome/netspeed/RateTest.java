package local.tvhome.netspeed;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RateTest {
    @Test
    public void bytesGrowFromBytesToKilobytesToMegabytes() {
        assertEquals("0 B/s", Rate.format(0, false));
        assertEquals("512 B/s", Rate.format(512, false));
        assertEquals("1.5 KB/s", Rate.format(1536, false));
        assertEquals("120 KB/s", Rate.format(120 * 1024, false));
        assertEquals("2.4 MB/s", Rate.format((long) (2.4 * 1024 * 1024), false));
        assertEquals("1.1 GB/s", Rate.format((long) (1.1 * 1024 * 1024 * 1024), false));
    }

    @Test
    public void bitsUseThousandsAndEightBitsAByte() {
        assertEquals("0 b/s", Rate.format(0, true));
        assertEquals("800 b/s", Rate.format(100, true));
        assertEquals("8.0 Kb/s", Rate.format(1000, true));
        assertEquals("80 Mb/s", Rate.format(10_000_000, true));
    }

    @Test
    public void theRateIsTheGrowthPerSecondAndNeverNegative() {
        assertEquals(10_000, Rate.perSecond(1000, 2000, 100));
        assertEquals(0, Rate.perSecond(5000, 1000, 100));   // a counter reset
        assertEquals(0, Rate.perSecond(1000, 2000, 0));
        assertEquals(0, Rate.perSecond(-1, 2000, 100));     // TrafficStats.UNSUPPORTED
    }

    @Test
    public void theIntervalsGoFromATenthOfASecondToTen() {
        assertEquals(100, Rate.INTERVALS_MS[0]);
        assertEquals(10_000, Rate.INTERVALS_MS[Rate.INTERVALS_MS.length - 1]);
    }
}
