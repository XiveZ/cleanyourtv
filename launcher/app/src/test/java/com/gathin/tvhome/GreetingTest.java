package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GreetingTest {
    private static int at(int h, int m) {
        return h * 60 + m;
    }

    // Minsk: 21 June sunrise 04:30, sunset 21:52; 21 December 09:19 and 16:56.
    private static final int SUMMER_RISE = at(4, 30), SUMMER_SET = at(21, 52);
    private static final int WINTER_RISE = at(9, 19), WINTER_SET = at(16, 56);

    @Test
    public void summerMorningStartsAnHourBeforeSunriseAndEndsAtEleven() {
        assertEquals(Greeting.Part.HELLO, Greeting.part(at(3, 0), SUMMER_RISE, SUMMER_SET));
        assertEquals(Greeting.Part.MORNING, Greeting.part(at(3, 30), SUMMER_RISE, SUMMER_SET));
        assertEquals(Greeting.Part.MORNING, Greeting.part(at(10, 59), SUMMER_RISE, SUMMER_SET));
        assertEquals(Greeting.Part.DAY, Greeting.part(at(11, 0), SUMMER_RISE, SUMMER_SET));
    }

    @Test
    public void summerEveningWaitsForSixEvenWithTheSunHigh() {
        assertEquals(Greeting.Part.DAY, Greeting.part(at(17, 59), SUMMER_RISE, SUMMER_SET));
        assertEquals(Greeting.Part.EVENING, Greeting.part(at(18, 0), SUMMER_RISE, SUMMER_SET));
        assertEquals(Greeting.Part.EVENING, Greeting.part(at(23, 59), SUMMER_RISE, SUMMER_SET));
        assertEquals(Greeting.Part.HELLO, Greeting.part(at(0, 0), SUMMER_RISE, SUMMER_SET));
    }

    @Test
    public void winterMorningStartsAtFiveAndLastsToNoonEveningComesAtFour() {
        assertEquals(Greeting.Part.HELLO, Greeting.part(at(4, 59), WINTER_RISE, WINTER_SET));
        assertEquals(Greeting.Part.MORNING, Greeting.part(at(5, 0), WINTER_RISE, WINTER_SET));
        assertEquals(Greeting.Part.MORNING, Greeting.part(at(11, 59), WINTER_RISE, WINTER_SET));
        assertEquals(Greeting.Part.DAY, Greeting.part(at(12, 0), WINTER_RISE, WINTER_SET));
        assertEquals(Greeting.Part.DAY, Greeting.part(at(15, 59), WINTER_RISE, WINTER_SET));
        assertEquals(Greeting.Part.EVENING, Greeting.part(at(16, 0), WINTER_RISE, WINTER_SET));
    }

    @Test
    public void autumnFollowsTheSunBetweenTheLimits() {
        // 28 October: sunrise 07:56, sunset 17:49 -> morning 05:00-10:56, evening from 16:49.
        int rise = at(7, 56), set = at(17, 49);
        assertEquals(Greeting.Part.MORNING, Greeting.part(at(10, 55), rise, set));
        assertEquals(Greeting.Part.DAY, Greeting.part(at(11, 0), rise, set));
        assertEquals(Greeting.Part.DAY, Greeting.part(at(16, 48), rise, set));
        assertEquals(Greeting.Part.EVENING, Greeting.part(at(16, 49), rise, set));
    }
}
