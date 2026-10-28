package local.tvhome.netspeed;

import java.util.Locale;

/** Bytes per second from two counter readings, and a short label for it. */
final class Rate {
    private Rate() { }

    /** Update every 0.1 s to 10 s. */
    static final int[] INTERVALS_MS = {100, 200, 500, 1000, 2000, 5000, 10_000};

    static long perSecond(long before, long now, long elapsedMs) {
        if (before < 0 || now < before || elapsedMs <= 0) return 0;
        return (now - before) * 1000 / elapsedMs;
    }

    /** "512 B/s", "1.5 KB/s", "2.4 MB/s" (1024s), or with {@code bits} "800 b/s", "8.0 Kb/s", "80 Mb/s" (1000s). */
    static String format(long bytesPerSecond, boolean bits) {
        String[] p = parts(bytesPerSecond, bits);
        return p[0] + " " + p[1];
    }

    /**
     * The same, always 9 characters: the number right-aligned in 4, the unit left-aligned in 4. Drawn in a monospace
     * font the line keeps its width, so only the digits and the unit change, nothing jumps.
     */
    static String fixed(long bytesPerSecond, boolean bits) {
        String[] p = parts(bytesPerSecond, bits);
        return String.format(Locale.ROOT, "%4s %-4s", p[0], p[1]);
    }

    /** {number, unit}: one decimal under 10, whole numbers from 10 up to the next unit. */
    private static String[] parts(long bytesPerSecond, boolean bits) {
        double v = bits ? bytesPerSecond * 8.0 : bytesPerSecond;
        double step = bits ? 1000 : 1024;
        String[] units = bits ? new String[]{"b/s", "Kb/s", "Mb/s", "Gb/s"} : new String[]{"B/s", "KB/s", "MB/s", "GB/s"};
        int u = 0;
        while (v >= step && u < units.length - 1) {
            v /= step;
            u++;
        }
        String number = u > 0 && v < 10 ? String.format(Locale.ROOT, "%.1f", v) : String.valueOf(Math.round(v));
        return new String[]{number, units[u]};
    }
}
