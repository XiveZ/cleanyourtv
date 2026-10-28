package com.gathin.tvhome;

import java.time.Duration;
import java.time.MonthDay;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/** The New Year countdown: time to midnight of 1 January (or another target day). */
final class NewYear {
    private NewYear() { }

    static final String[] TARGETS = {"new_year", "christmas", "orthodox_christmas"};
    /** tree, star (the carollers' star), pavuk (the straw "spider" of BY, PL, LT, LV, UA), didukh (UA), snow. */
    static final String[] STYLES = {"tree", "star", "pavuk", "didukh", "snow"};

    static long secondsLeft(LocalDateTime now) {
        return secondsLeft(now, MonthDay.of(1, 1));
    }

    /**
     * Seconds to midnight starting {@code day}. The first five minutes of that day (Celebration) count as zero;
     * after them the count runs to the same day next year.
     */
    static long secondsLeft(LocalDateTime now, MonthDay day) {
        if (Celebration.secondsIn(now, day) >= 0) return 0;
        LocalDateTime next = day.atYear(now.getYear()).atStartOfDay();
        if (!next.isAfter(now)) next = day.atYear(now.getYear() + 1).atStartOfDay();
        return Duration.between(now, next).getSeconds();
    }

    /** new_year_target: 1 January, 25 December (Christmas) or 7 January (Orthodox Christmas). */
    static MonthDay target(String name) {
        if ("christmas".equals(name)) return MonthDay.of(12, 25);
        if ("orthodox_christmas".equals(name)) return MonthDay.of(1, 7);
        return MonthDay.of(1, 1);
    }

    static String style(String name) {
        for (String s : STYLES) if (s.equals(name)) return s;
        return "tree";
    }

    /** Days, and the rest as "HH:MM:SS". */
    static final class Left {
        final long days;
        final String clock;

        Left(long days, String clock) {
            this.days = days;
            this.clock = clock;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Left && ((Left) o).days == days && ((Left) o).clock.equals(clock);
        }

        @Override
        public int hashCode() {
            return Objects.hash(days, clock);
        }

        @Override
        public String toString() {
            return days + " " + clock;
        }
    }

    static Left split(long seconds) {
        long s = seconds % 86400;
        return new Left(seconds / 86400,
                String.format(Locale.ROOT, "%02d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60));
    }
}
