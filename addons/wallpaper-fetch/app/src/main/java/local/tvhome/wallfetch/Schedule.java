package local.tvhome.wallfetch;

/** The update period, in days (0 is off). */
final class Schedule {
    private Schedule() { }

    static final int[] DAYS = {0, 7, 30, 365, 1, 3, 14, 90, 180};
    static final int[] COUNTS = {20, 40, 60, 100};

    static long periodMs(int days) {
        return days * 24L * 60 * 60 * 1000;
    }

    static boolean due(long last, long now, int days) {
        return days > 0 && (last <= 0 || now - last >= periodMs(days));
    }
}
