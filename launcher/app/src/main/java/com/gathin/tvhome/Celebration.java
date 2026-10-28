package com.gathin.tvhome;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.MonthDay;

/**
 * The five minutes after the countdown reaches its day (new_year_target): the order of the scenes and where each
 * figure of the Kaliady procession is. Pure timing, drawn by CelebrationView.
 *
 * Figures: 0 Ziuzia (the Belarusian winter lord, white beard, bare feet, a mace), 1 the goat (koza, led around at
 * Kaliady), 2 the wolf, 3 the bear, 4 Kuttsia (the Kaliady Eve dish, here a pot that dances).
 */
final class Celebration {
    private Celebration() { }

    static final int LENGTH = 300;

    enum Scene { GREETING, ZIUZIA, KALIADY, KUTTSIA, DANCE, FAREWELL }

    /** Scene start times in seconds, in order. */
    private static final int[] STARTS = {0, 45, 105, 165, 210, 270};
    /** When each figure comes in, the side it comes from (-1 left, 1 right) and its place across the screen. */
    private static final int[] ENTERS = {45, 105, 115, 125, 165};
    private static final int[] SIDE = {-1, 1, -1, 1, 1};
    private static final float[] SLOT = {0.13f, 0.87f, 0.31f, 0.69f, 0.5f};
    static final int FIGURES = ENTERS.length;
    private static final float WALK = 6, LEAVE_FROM = 280, LEAVE = 14;

    /** Seconds since the target day began, while that is under five minutes; else -1. */
    static double secondsIn(LocalDateTime now, MonthDay target) {
        if (!MonthDay.from(now).equals(target)) return -1;
        double s = Duration.between(now.toLocalDate().atStartOfDay(), now).toMillis() / 1000.0;
        return s < LENGTH ? s : -1;
    }

    static Scene scene(double s) {
        Scene[] all = Scene.values();
        for (int i = all.length - 1; i >= 0; i--) if (s >= STARTS[i]) return all[i];
        return Scene.GREETING;
    }

    /** 0..1 through the current scene. */
    static float progress(double s) {
        int i = scene(s).ordinal();
        int end = i + 1 < STARTS.length ? STARTS[i + 1] : LENGTH;
        return (float) Math.max(0, Math.min(1, (s - STARTS[i]) / (end - STARTS[i])));
    }

    /** Centre x of figure {@code i}: off screen until it walks in, swaying in place, then walking out its side. */
    static float figureX(int i, double s, float width) {
        float off = SIDE[i] < 0 ? -0.15f * width : 1.15f * width;
        float home = SLOT[i] * width;
        if (s < ENTERS[i]) return off;
        if (s < ENTERS[i] + WALK) return lerp(off, home, ease((float) ((s - ENTERS[i]) / WALK)));
        float sway = (float) Math.sin(s * 1.7 + i) * 0.015f * width;
        if (s < LEAVE_FROM) return home + sway;
        float out = home < width / 2 ? -0.2f * width : 1.2f * width;
        return lerp(home + sway, out, ease((float) Math.min(1, (s - LEAVE_FROM) / LEAVE)));
    }

    /** How high figure {@code i} hops (0..1), faster in the dance. */
    static float hop(int i, double s) {
        double speed = scene(s) == Scene.DANCE ? 5.5 : 3.2;
        return (float) Math.abs(Math.sin(s * speed + i * 0.9));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float ease(float t) {
        return t * t * (3 - 2 * t);
    }
}
