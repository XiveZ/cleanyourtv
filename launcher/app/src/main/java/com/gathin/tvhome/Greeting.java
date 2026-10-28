package com.gathin.tvhome;

/**
 * The part of the day for the greeting, from the sun at the TV's place: morning from an hour before sunrise (not
 * before 03:30, at the latest 05:00) to three hours after it (11:00 to 12:00), evening from an hour before sunset
 * (16:00 to 18:00) to midnight, day between. After midnight it is a plain hello: "good night" says goodbye.
 */
final class Greeting {
    private Greeting() { }

    enum Part { HELLO, MORNING, DAY, EVENING }

    static Part part(int minute, int sunrise, int sunset) {
        int morning = clamp(sunrise - 60, 3 * 60 + 30, 5 * 60);
        int day = clamp(sunrise + 3 * 60, 11 * 60, 12 * 60);
        int evening = clamp(sunset - 60, 16 * 60, 18 * 60);
        if (minute < morning) return Part.HELLO;
        if (minute < day) return Part.MORNING;
        if (minute < evening) return Part.DAY;
        return Part.EVENING;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
