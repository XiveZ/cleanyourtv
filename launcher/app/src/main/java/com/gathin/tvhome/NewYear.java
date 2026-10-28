package com.gathin.tvhome;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

/** The New Year countdown: time to midnight of 1 January; all of New Year's Day counts as zero. */
final class NewYear {
    private NewYear() { }

    static long secondsLeft(LocalDateTime now) {
        if (now.getMonthValue() == 1 && now.getDayOfMonth() == 1) return 0;
        LocalDateTime next = LocalDate.of(now.getYear() + 1, 1, 1).atStartOfDay();
        return Duration.between(now, next).getSeconds();
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
