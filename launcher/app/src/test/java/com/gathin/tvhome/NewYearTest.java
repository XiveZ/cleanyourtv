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
    public void onNewYearsDayTheCountIsZeroThenTheNextYearStarts() {
        assertEquals(0, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 1, 0, 0, 0)));
        assertEquals(0, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 1, 23, 59, 59)));
        assertEquals(364L * 86400 - 1, NewYear.secondsLeft(LocalDateTime.of(2027, 1, 2, 0, 0, 1)));
    }

    @Test
    public void theTimerReadsDaysThenHoursMinutesSeconds() {
        assertEquals(new NewYear.Left(64, "12:30:15"), NewYear.split(64L * 86400 + 12 * 3600 + 30 * 60 + 15));
        assertEquals(new NewYear.Left(0, "00:00:01"), NewYear.split(1));
    }
}
