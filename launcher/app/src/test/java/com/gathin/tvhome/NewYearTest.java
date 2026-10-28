package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;

import org.junit.Test;

public class NewYearTest {
    @Test
    public void theCountdownRunsToMidnightOfTheFirstOfJanuary() {
        assertEquals(64L * 86400 + 12 * 3600 + 30 * 60 + 15,
                NewYear.secondsLeft(LocalDateTime.of(2026, 10, 28, 11, 29, 45)));
        assertEquals(1, NewYear.secondsLeft(LocalDateTime.of(2026, 12, 31, 23, 59, 59)));
    }

    @Test
    public void theCountIsZeroForTheFiveMinutesOfCelebrationThenTheNextYearStarts() {
        assertEquals(0, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 1, 0, 0, 0)));
        assertEquals(0, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 1, 0, 4, 59)));
        assertEquals(365L * 86400 - 300, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 1, 0, 5, 0)));
        assertEquals(364L * 86400 - 1, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 2, 0, 0, 1)));
    }

    @Test
    public void theTimerReadsDaysThenHoursMinutesSeconds() {
        assertEquals(new NewYear.Left(64, "12:30:15"), NewYear.split(64L * 86400 + 12 * 3600 + 30 * 60 + 15));
        assertEquals(new NewYear.Left(0, "00:00:01"), NewYear.split(1));
    }

    @Test
    public void theCountdownCanRunToEitherChristmas() {
        java.time.MonthDay catholic = java.time.MonthDay.of(12, 25), orthodox = java.time.MonthDay.of(1, 7);
        LocalDateTime now = LocalDateTime.of(2026, 10, 28, 12, 0, 0);
        assertEquals(57L * 86400 + 12 * 3600, NewYear.secondsLeft(now, catholic));
        assertEquals(70L * 86400 + 12 * 3600, NewYear.secondsLeft(now, orthodox));
        // On the day itself: zero for the five minutes of celebration, then a year to go.
        assertEquals(0, NewYear.secondsLeft(LocalDateTime.of(2026, 12, 25, 0, 2), catholic));
        assertEquals(365L * 86400 - 9 * 3600, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 7, 9, 0), orthodox));
        assertEquals(364L * 86400, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 8, 0, 0), orthodox));
    }

    @Test
    public void theTargetAndStyleNamesAreReadSafely() {
        assertEquals(java.time.MonthDay.of(1, 1), NewYear.target("new_year"));
        assertEquals(java.time.MonthDay.of(12, 25), NewYear.target("christmas"));
        assertEquals(java.time.MonthDay.of(1, 7), NewYear.target("orthodox_christmas"));
        assertEquals(java.time.MonthDay.of(1, 1), NewYear.target("bogus"));
        assertEquals("pavuk", NewYear.style("pavuk"));
        assertEquals("tree", NewYear.style("bogus"));
        assertEquals("tree", NewYear.style(null));
    }
}
