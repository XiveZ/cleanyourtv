package com.gathin.tvhome;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The sky for one day at one place, worked out offline: the sun (Sun, NOAA), the moon's rise, set, phase and next
 * new or full moon (Paul Schlyter's low-precision orbit with its main perturbations, a few minutes off at most), and
 * the meteor showers active that day (the IMO calendar's usual dates). Pure maths, no Android types.
 */
final class Astro {
    private Astro() { }

    /** Minutes of the day, or -1 when it does not happen that day. */
    static final class Day {
        int sunRise = -1, sunSet = -1, moonRise = -1, moonSet = -1;
        /** Lit share of the moon at noon, 0 to 1. */
        double illumination;
        boolean waxing;
        LocalDate nextPhaseDate;
        boolean nextIsFull;
    }

    enum Phase { NEW, WAXING_CRESCENT, FIRST_QUARTER, WAXING_GIBBOUS, FULL, WANING_GIBBOUS, LAST_QUARTER, WANING_CRESCENT }

    /** The phase's name from the lit share (0..1) and whether it grows: new under 3%, quarter 40-60%, full over 97%. */
    static Phase phase(double lit, boolean waxing) {
        if (lit < 0.03) return Phase.NEW;
        if (lit > 0.97) return Phase.FULL;
        if (lit >= 0.4 && lit <= 0.6) return waxing ? Phase.FIRST_QUARTER : Phase.LAST_QUARTER;
        if (lit < 0.4) return waxing ? Phase.WAXING_CRESCENT : Phase.WANING_CRESCENT;
        return waxing ? Phase.WAXING_GIBBOUS : Phase.WANING_GIBBOUS;
    }

    static Day day(LocalDate date, double lat, double lon, ZoneId zone) {
        Day d = new Day();
        int[] sun = Sun.riseAndSet(date, lat, lon, zone);
        if (sun != null) {
            d.sunRise = sun[0];
            d.sunSet = sun[1];
        }
        long midnight = date.atStartOfDay(zone).toInstant().toEpochMilli();
        moonRiseSet(d, midnight, lat, lon);
        double[] noon = elongation(days(midnight + 12 * 3_600_000L));
        d.illumination = (1 - Math.cos(Math.toRadians(noon[0]))) / 2;
        d.waxing = noon[1] < 180;
        nextPhase(d, midnight + 12 * 3_600_000L, zone);
        return d;
    }

    // ---- moon rise and set: the altitude every 10 minutes, a crossing interpolated ---------------------------

    private static void moonRiseSet(Day d, long midnight, double lat, double lon) {
        final int step = 10;
        double prev = moonAltitude(days(midnight), lat, lon);
        for (int m = step; m <= 24 * 60; m += step) {
            double alt = moonAltitude(days(midnight + m * 60_000L), lat, lon);
            if ((prev < 0) != (alt < 0)) {
                int at = (int) Math.round(m - step + step * prev / (prev - alt));
                if (prev < 0 && d.moonRise < 0) d.moonRise = at;
                if (prev >= 0 && d.moonSet < 0) d.moonSet = at;
            }
            prev = alt;
        }
    }

    /** Geocentric altitude above the moon's rise horizon (0.7275 parallax - 34'), degrees. */
    private static double moonAltitude(double d, double lat, double lon) {
        double[] m = moon(d);   // ra, dec, distance in earth radii
        double parallax = Math.toDegrees(Math.asin(1 / m[2]));
        double ut = (d - Math.floor(d)) * 24;
        double ls = rev(sunM(d) + sunW(d));
        double lst = rev(ls + 180 + ut * 15 + lon);
        double ha = Math.toRadians(lst - m[0]);
        double phi = Math.toRadians(lat), dec = Math.toRadians(m[1]);
        double alt = Math.toDegrees(Math.asin(Math.sin(phi) * Math.sin(dec) + Math.cos(phi) * Math.cos(dec) * Math.cos(ha)));
        return alt - (0.7275 * parallax - 34 / 60.0);
    }

    // ---- phase -------------------------------------------------------------------------------------------

    /** {elongation 0..180, moon minus sun longitude 0..360}. */
    private static double[] elongation(double d) {
        double[] ml = moonEcliptic(d);
        double sl = sunLongitude(d);
        double diff = rev(ml[0] - sl);
        double e = Math.toDegrees(Math.acos(Math.cos(Math.toRadians(diff)) * Math.cos(Math.toRadians(ml[1]))));
        return new double[]{e, diff};
    }

    /** The first new (0) or full (180) moon after {@code from}, hour by hour over 31 days. */
    private static void nextPhase(Day d, long from, ZoneId zone) {
        double prev = elongation(days(from))[1];
        for (int h = 1; h <= 31 * 24; h++) {
            long t = from + h * 3_600_000L;
            double now = elongation(days(t))[1];
            boolean full = prev < 180 && now >= 180;
            boolean fresh = prev > now;   // wrapped past 360: new moon
            if (full || fresh) {
                d.nextIsFull = full;
                d.nextPhaseDate = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(t), zone).toLocalDate();
                return;
            }
            prev = now;
        }
    }

    // ---- Schlyter's orbits; d = days since 2000 Jan 0.0 UT ---------------------------------------------------

    static double days(long epochMillis) {
        return epochMillis / 86_400_000.0 + 2440587.5 - 2451543.5;
    }

    private static double sunW(double d) {
        return 282.9404 + 4.70935e-5 * d;
    }

    private static double sunM(double d) {
        return rev(356.0470 + 0.9856002585 * d);
    }

    private static double sunLongitude(double d) {
        double e = 0.016709 - 1.151e-9 * d, m = Math.toRadians(sunM(d));
        double ea = m + e * Math.sin(m) * (1 + e * Math.cos(m));
        double xv = Math.cos(ea) - e, yv = Math.sqrt(1 - e * e) * Math.sin(ea);
        return rev(Math.toDegrees(Math.atan2(yv, xv)) + sunW(d));
    }

    /** {longitude, latitude, distance in earth radii}, ecliptic of date. */
    private static double[] moonEcliptic(double d) {
        double n = rev(125.1228 - 0.0529538083 * d), i = 5.1454, w = rev(318.0634 + 0.1643573223 * d);
        double a = 60.2666, e = 0.054900, m = rev(115.3654 + 13.0649929509 * d);
        double mr = Math.toRadians(m);
        double ea = mr + e * Math.sin(mr) * (1 + e * Math.cos(mr));
        for (int k = 0; k < 5; k++) ea = ea - (ea - e * Math.sin(ea) - mr) / (1 - e * Math.cos(ea));
        double xv = a * (Math.cos(ea) - e), yv = a * Math.sqrt(1 - e * e) * Math.sin(ea);
        double v = Math.toDegrees(Math.atan2(yv, xv)), r = Math.sqrt(xv * xv + yv * yv);
        double nr = Math.toRadians(n), vw = Math.toRadians(v + w), ir = Math.toRadians(i);
        double xh = r * (Math.cos(nr) * Math.cos(vw) - Math.sin(nr) * Math.sin(vw) * Math.cos(ir));
        double yh = r * (Math.sin(nr) * Math.cos(vw) + Math.cos(nr) * Math.sin(vw) * Math.cos(ir));
        double zh = r * Math.sin(vw) * Math.sin(ir);
        double lon = Math.toDegrees(Math.atan2(yh, xh)), lat = Math.toDegrees(Math.atan2(zh, Math.hypot(xh, yh)));
        double ms = sunM(d), ls = rev(ms + sunW(d)), lm = rev(m + w + n), dd = rev(lm - ls), f = rev(lm - n);
        lon += -1.274 * sin(m - 2 * dd) + 0.658 * sin(2 * dd) - 0.186 * sin(ms) - 0.059 * sin(2 * m - 2 * dd)
                - 0.057 * sin(m - 2 * dd + ms) + 0.053 * sin(m + 2 * dd) + 0.046 * sin(2 * dd - ms)
                + 0.041 * sin(m - ms) - 0.035 * sin(dd) - 0.031 * sin(m + ms) - 0.015 * sin(2 * f - 2 * dd)
                + 0.011 * sin(m - 4 * dd);
        lat += -0.173 * sin(f - 2 * dd) - 0.055 * sin(m - f - 2 * dd) - 0.046 * sin(m + f - 2 * dd)
                + 0.033 * sin(f + 2 * dd) + 0.017 * sin(2 * m + f);
        r += -0.58 * Math.cos(Math.toRadians(m - 2 * dd)) - 0.46 * Math.cos(Math.toRadians(2 * dd));
        return new double[]{rev(lon), lat, r};
    }

    /** {right ascension, declination} in degrees, and distance in earth radii. */
    private static double[] moon(double d) {
        double[] ec = moonEcliptic(d);
        double lon = Math.toRadians(ec[0]), lat = Math.toRadians(ec[1]), ecl = Math.toRadians(23.4393 - 3.563e-7 * d);
        double xg = Math.cos(lon) * Math.cos(lat), yg = Math.sin(lon) * Math.cos(lat), zg = Math.sin(lat);
        double ye = yg * Math.cos(ecl) - zg * Math.sin(ecl), ze = yg * Math.sin(ecl) + zg * Math.cos(ecl);
        return new double[]{rev(Math.toDegrees(Math.atan2(ye, xg))), Math.toDegrees(Math.atan2(ze, Math.hypot(xg, ye))), ec[2]};
    }

    private static double sin(double degrees) {
        return Math.sin(Math.toRadians(degrees));
    }

    private static double rev(double x) {
        return x - Math.floor(x / 360) * 360;
    }

    // ---- meteor showers ------------------------------------------------------------------------------------

    static final class Shower {
        final String key;
        final MonthDay start, peak, end;
        final int zhr;
        boolean peakToday;

        Shower(String key, String start, String peak, String end, int zhr) {
            this.key = key;
            this.start = MonthDay.parse("--" + start);
            this.peak = MonthDay.parse("--" + peak);
            this.end = MonthDay.parse("--" + end);
            this.zhr = zhr;
        }

        boolean activeOn(MonthDay day) {
            // A shower can run across the new year (the Quadrantids).
            return start.isAfter(end) ? !day.isBefore(start) || !day.isAfter(end)
                    : !day.isBefore(start) && !day.isAfter(end);
        }
    }

    /** The major annual showers (IMO calendar): active days, peak, zenithal hourly rate. */
    private static List<Shower> all() {
        return Arrays.asList(
                new Shower("Quadrantids", "12-28", "01-03", "01-12", 110),
                new Shower("Lyrids", "04-14", "04-22", "04-30", 18),
                new Shower("Eta Aquariids", "04-19", "05-06", "05-28", 50),
                new Shower("Southern Delta Aquariids", "07-12", "07-30", "08-23", 25),
                new Shower("Perseids", "07-17", "08-12", "08-24", 100),
                new Shower("Draconids", "10-06", "10-08", "10-10", 10),
                new Shower("Southern Taurids", "09-10", "10-10", "11-20", 5),
                new Shower("Orionids", "10-02", "10-21", "11-07", 20),
                new Shower("Northern Taurids", "10-20", "11-12", "12-10", 5),
                new Shower("Leonids", "11-06", "11-17", "11-30", 15),
                new Shower("Geminids", "12-04", "12-14", "12-20", 150),
                new Shower("Ursids", "12-17", "12-22", "12-26", 10));
    }

    /** Showers active on {@code date}, strongest first (a peak day counts as strongest). */
    static List<Shower> showers(LocalDate date) {
        MonthDay day = MonthDay.from(date);
        List<Shower> on = new ArrayList<>();
        for (Shower s : all()) {
            if (!s.activeOn(day)) continue;
            s.peakToday = s.peak.equals(day);
            on.add(s);
        }
        on.sort((a, b) -> a.peakToday != b.peakToday ? (a.peakToday ? -1 : 1) : Integer.compare(b.zhr, a.zhr));
        return on;
    }
}
