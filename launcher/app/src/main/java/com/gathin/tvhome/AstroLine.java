package com.gathin.tvhome;

import android.content.Context;
import android.content.res.Resources;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

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
    private CharSequence text = "";
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

    /** The lines for today, with icons; recalculated only when the date changed. */
    CharSequence text(Context ctx) {
        refresh(ctx);
        return text;
    }

    private void refresh(Context ctx) {
        LocalDate today = LocalDate.now();
        if (!placed || (today.equals(made) && (ctx == null || text.length() > 0))) return;
        Astro.Day d = Astro.day(today, lat, lon, ZoneId.systemDefault());
        sun = d.sunRise >= 0 && d.sunSet >= 0 ? new int[]{d.sunRise, d.sunSet} : null;
        made = today;
        if (ctx != null) text = lines(ctx, d, today);
    }

    private static CharSequence lines(Context ctx, Astro.Day d, LocalDate today) {
        android.content.res.Resources res = ctx.getResources();
        DateTimeFormatter dm = DateTimeFormatter.ofPattern("dd.MM", Locale.ROOT);
        SpannableStringBuilder sb = new SpannableStringBuilder();
        icon(sb, ctx.getDrawable(R.drawable.ic_astro_sunrise));
        sb.append(time(d.sunRise)).append("    ");
        icon(sb, ctx.getDrawable(R.drawable.ic_astro_sunset));
        sb.append(time(d.sunSet));
        if (d.sunRise >= 0 && d.sunSet >= 0) {
            sb.append("   \u00B7 ").append(res.getString(R.string.astro_day,
                    res.getString(R.string.astro_hours, (d.sunSet - d.sunRise) / 60, (d.sunSet - d.sunRise) % 60)));
        }
        sb.append('\n');
        icon(sb, new MoonPhaseDrawable(d.illumination, d.waxing));
        sb.append(res.getString(phaseName(Astro.phase(d.illumination, d.waxing)))).append(", ")
                .append(String.valueOf(Math.round(d.illumination * 100))).append("%    ");
        icon(sb, ctx.getDrawable(R.drawable.ic_astro_moonrise));
        sb.append(time(d.moonRise)).append("    ");
        icon(sb, ctx.getDrawable(R.drawable.ic_astro_moonset));
        sb.append(time(d.moonSet));
        if (d.nextPhaseDate != null) {
            sb.append("   \u00B7 ").append(res.getString(d.nextIsFull ? R.string.astro_full : R.string.astro_new))
                    .append(' ').append(d.nextPhaseDate.format(dm));
        }
        List<String> names = new ArrayList<>();
        for (Astro.Shower s : Astro.showers(today)) {
            String name = res.getString(name(s.key));
            names.add(s.peakToday ? res.getString(R.string.astro_peak_today, name)
                    : res.getString(R.string.astro_peak_on, name, s.peak.atYear(today.getYear()).format(dm)));
            if (names.size() == 2) break;
        }
        if (!names.isEmpty()) {
            sb.append('\n');
            icon(sb, ctx.getDrawable(R.drawable.ic_astro_meteor));
            sb.append(String.join(", ", names));
        }
        return sb;
    }

    /** An icon and a space before the value it marks. */
    private static void icon(SpannableStringBuilder sb, android.graphics.drawable.Drawable d) {
        int at = sb.length();
        sb.append('\uFFFC');
        sb.setSpan(new IconSpan(d), at, at + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.append(' ');
    }

    private static int phaseName(Astro.Phase p) {
        switch (p) {
            case NEW: return R.string.phase_new;
            case WAXING_CRESCENT: return R.string.phase_waxing_crescent;
            case FIRST_QUARTER: return R.string.phase_first_quarter;
            case WAXING_GIBBOUS: return R.string.phase_waxing_gibbous;
            case FULL: return R.string.phase_full;
            case WANING_GIBBOUS: return R.string.phase_waning_gibbous;
            case LAST_QUARTER: return R.string.phase_last_quarter;
            default: return R.string.phase_waning_crescent;
        }
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
