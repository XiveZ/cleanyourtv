package com.gathin.tvhome;

import android.content.res.Resources;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * show_astro: the lines under the clock, worked out offline (Astro) for config.xml's latitude and longitude.
 * Made once a day: the first time Home shows after midnight, or after the TV was off for weeks.
 */
final class AstroLine {
    private final double lat, lon;
    private final boolean placed;
    private LocalDate made;
    private String text = "";
    private int[] sun;

    AstroLine(Resources res) {
        double la = Double.NaN, lo = Double.NaN;
        try {
            la = Double.parseDouble(res.getString(R.string.latitude).trim());
            lo = Double.parseDouble(res.getString(R.string.longitude).trim());
        } catch (NumberFormatException ignored) {
            // no place set: no astro lines; the greeting uses WallpaperMood's default sun
        }
        lat = la;
        lon = lo;
        placed = !Double.isNaN(la) && !Double.isNaN(lo);
    }

    boolean placed() {
        return placed;
    }

    /** Today's {sunrise, sunset} in minutes, or null without a place or in polar day or night. */
    int[] sun() {
        refresh(null);
        return sun;
    }

    /** The lines for today; recalculated only when the date changed. */
    String text(Resources res) {
        refresh(res);
        return text;
    }

    private void refresh(Resources res) {
        LocalDate today = LocalDate.now();
        if (!placed || (today.equals(made) && (res == null || !text.isEmpty()))) return;
        Astro.Day d = Astro.day(today, lat, lon, ZoneId.systemDefault());
        sun = d.sunRise >= 0 && d.sunSet >= 0 ? new int[]{d.sunRise, d.sunSet} : null;
        made = today;
        if (res != null) text = lines(res, d, today);
    }

    private static String lines(Resources res, Astro.Day d, LocalDate today) {
        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd.MM", Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        String length = d.sunRise >= 0 && d.sunSet >= 0
                ? res.getString(R.string.astro_hours, (d.sunSet - d.sunRise) / 60, (d.sunSet - d.sunRise) % 60) : "—";
        sb.append(res.getString(R.string.astro_sun, time(d.sunRise), time(d.sunSet), length));
        String next = d.nextPhaseDate == null ? "" : d.nextPhaseDate.format(dm);
        sb.append('\n').append(res.getString(R.string.astro_moon,
                res.getString(d.waxing ? R.string.astro_waxing : R.string.astro_waning),
                Math.round(d.illumination * 100), time(d.moonRise), time(d.moonSet),
                res.getString(d.nextIsFull ? R.string.astro_full : R.string.astro_new), next));
        List<String> names = new ArrayList<>();
        for (Astro.Shower s : Astro.showers(today)) {
            String name = res.getString(name(s.key));
            names.add(s.peakToday ? res.getString(R.string.astro_peak_today, name)
                    : res.getString(R.string.astro_peak_on, name, s.peak.atYear(today.getYear()).format(dm)));
            if (names.size() == 2) break;
        }
        if (!names.isEmpty()) sb.append('\n').append(res.getString(R.string.astro_showers, String.join(", ", names)));
        return sb.toString();
    }

    private static String time(int minute) {
        return minute < 0 ? "—" : String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
    }

    private static int name(String key) {
        switch (key) {
            case "Quadrantids": return R.string.shower_quadrantids;
            case "Lyrids": return R.string.shower_lyrids;
            case "Eta Aquariids": return R.string.shower_eta_aquariids;
            case "Southern Delta Aquariids": return R.string.shower_southern_delta_aquariids;
            case "Perseids": return R.string.shower_perseids;
            case "Draconids": return R.string.shower_draconids;
            case "Southern Taurids": return R.string.shower_southern_taurids;
            case "Orionids": return R.string.shower_orionids;
            case "Northern Taurids": return R.string.shower_northern_taurids;
            case "Leonids": return R.string.shower_leonids;
            case "Geminids": return R.string.shower_geminids;
            default: return R.string.shower_ursids;
        }
    }
}
