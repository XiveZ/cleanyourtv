package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.MonthDay;

import org.junit.Test;

public class CelebrationTest {
    private static final MonthDay NEW_YEAR = MonthDay.of(1, 1);

    @Test
    public void theCelebrationRunsForTheFirstFiveMinutesOfTheDay() {
        assertEquals(-1, Celebration.secondsIn(LocalDateTime.of(2026, 12, 31, 23, 59, 59), NEW_YEAR), 1e-9);
        assertEquals(0, Celebration.secondsIn(LocalDateTime.of(2027, 1, 1, 0, 0, 0), NEW_YEAR), 1e-9);
        assertEquals(299, Celebration.secondsIn(LocalDateTime.of(2027, 1, 1, 0, 4, 59), NEW_YEAR), 1e-9);
        assertEquals(-1, Celebration.secondsIn(LocalDateTime.of(2027, 1, 1, 0, 5, 0), NEW_YEAR), 1e-9);
        assertEquals(-1, Celebration.secondsIn(LocalDateTime.of(2027, 1, 1, 12, 0, 0), NEW_YEAR), 1e-9);
    }

    @Test
    public void theScenesFollowEachOtherAndCoverTheFiveMinutes() {
        assertEquals(Celebration.Scene.GREETING, Celebration.scene(0));
        assertEquals(Celebration.Scene.ZIUZIA, Celebration.scene(50));
        assertEquals(Celebration.Scene.KALIADY, Celebration.scene(110));
        assertEquals(Celebration.Scene.KUTTSIA, Celebration.scene(170));
        assertEquals(Celebration.Scene.DANCE, Celebration.scene(215));
        assertEquals(Celebration.Scene.FAREWELL, Celebration.scene(285));
        assertEquals(Celebration.Scene.FAREWELL, Celebration.scene(299.9));
    }

    @Test
    public void eachFigureComesInOnItsTurnDancesAndLeavesAtTheEnd() {
        float w = 1920;
        // Ziuzia (figure 0) is off screen before his scene, in place while dancing, gone at the end.
        assertTrue(Celebration.figureX(0, 10, w) < 0 || Celebration.figureX(0, 10, w) > w);
        float dancing = Celebration.figureX(0, 240, w);
        assertTrue(dancing > 0 && dancing < w);
        float end = Celebration.figureX(0, 300, w);
        assertTrue(end < -100 || end > w + 100);
        // The goat (figure 1) joins in the Kaliady scene, not before.
        assertTrue(Celebration.figureX(1, 60, w) < 0 || Celebration.figureX(1, 60, w) > w);
        float goat = Celebration.figureX(1, 140, w);
        assertTrue(goat > 0 && goat < w);
    }
}
