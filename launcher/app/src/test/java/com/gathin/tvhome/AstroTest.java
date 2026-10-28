package com.gathin.tvhome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.Test;

/** Reference values for Minsk (53.9045 N, 27.5615 E) from PyEphem 4.2.1, USNO convention (pressure 0, horizon -0:34). */
public class AstroTest {
    private static final double LAT = 53.9045, LON = 27.5615;
    private static final ZoneId MINSK = ZoneId.of("Europe/Minsk");

    private static int at(int h, int m) {
        return h * 60 + m;
    }

    private static void near(String what, int expected, int actual, int minutes) {
        assertTrue(what + ": expected " + expected + " got " + actual, Math.abs(expected - actual) <= minutes);
    }

    private static void check(String date, int rise, int set, double illum, boolean waxing) {
        Astro.Day d = Astro.day(LocalDate.parse(date), LAT, LON, MINSK);
        if (rise >= 0) near(date + " moonrise", rise, d.moonRise, 8); else assertEquals(-1, d.moonRise);
        if (set >= 0) near(date + " moonset", set, d.moonSet, 8); else assertEquals(-1, d.moonSet);
        assertEquals(date + " illumination", illum, d.illumination * 100, 3);
        assertEquals(date + " waxing", waxing, d.waxing);
    }

    @Test
    public void theMoonMatchesTheReference() {
        check("2026-10-28", at(18, 14), at(11, 35), 93.2, false);
        check("2026-06-21", at(12, 43), at(0, 44), 44.5, true);
        check("2026-12-21", at(14, 1), at(6, 22), 89.4, true);
        check("2026-03-20", at(7, 20), at(21, 44), 2.4, true);
        check("2027-01-15", at(11, 30), at(1, 4), 45.0, true);
    }

    @Test
    public void theSunAndTheDayLengthMatchTheReference() {
        Astro.Day d = Astro.day(LocalDate.parse("2026-10-28"), LAT, LON, MINSK);
        near("sunrise", at(8, 2), d.sunRise, 3);
        near("sunset", at(17, 44), d.sunSet, 3);
        near("day length", at(9, 42), d.sunSet - d.sunRise, 6);   // two ends, each within 3
    }

    @Test
    public void theNextFullOrNewMoonIsFound() {
        Astro.Day d = Astro.day(LocalDate.parse("2026-10-28"), LAT, LON, MINSK);
        assertNotNull(d.nextPhaseDate);
        // Waning after the 26 October full moon: the next one is the new moon of 9 November 2026.
        assertFalse(d.nextIsFull);
        assertEquals(LocalDate.parse("2026-11-09"), d.nextPhaseDate);
    }

    @Test
    public void meteorShowersActiveTodayComeWithTheStrongestFirst() {
        List<Astro.Shower> on = Astro.showers(LocalDate.parse("2026-10-28"));
        assertEquals("Orionids", on.get(0).key);
        assertTrue(on.stream().anyMatch(s -> s.key.equals("Northern Taurids")));
        List<Astro.Shower> peak = Astro.showers(LocalDate.parse("2026-08-12"));
        assertEquals("Perseids", peak.get(0).key);
        assertTrue(peak.get(0).peakToday);
        // The Quadrantids run across the new year.
        assertEquals("Quadrantids", Astro.showers(LocalDate.parse("2027-01-03")).get(0).key);
        assertTrue(Astro.showers(LocalDate.parse("2026-02-15")).isEmpty());
    }

    private static Astro.Phase phaseOn(String date) {
        Astro.Day d = Astro.day(LocalDate.parse(date), LAT, LON, MINSK);
        return Astro.phase(d.illumination, d.waxing);
    }

    @Test
    public void thePhaseIsNamedFromTheLitShareAndItsGrowth() {
        assertEquals(Astro.Phase.NEW, phaseOn("2026-11-09"));
        assertEquals(Astro.Phase.WAXING_CRESCENT, phaseOn("2026-11-12"));
        assertEquals(Astro.Phase.WAXING_GIBBOUS, phaseOn("2026-11-20"));
        assertEquals(Astro.Phase.FULL, phaseOn("2026-11-24"));
        assertEquals(Astro.Phase.WANING_GIBBOUS, phaseOn("2026-11-28"));
        assertEquals(Astro.Phase.WANING_CRESCENT, phaseOn("2026-12-06"));
        assertEquals(Astro.Phase.FIRST_QUARTER, Astro.phase(0.5, true));
        assertEquals(Astro.Phase.LAST_QUARTER, Astro.phase(0.5, false));
    }
}
