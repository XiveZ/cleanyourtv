package com.gathin.tvhome;

/** The widgets' backdrop colour: "#RRGGBB" with an opacity in percent; black when the colour does not read. */
final class Backdrop {
    private Backdrop() { }

    static final String[] COLORS = {"#000000", "#141413", "#0B1026", "#1B2A41", "#3B2F8F", "#D97757", "#FFFFFF"};

    static int argb(String hex, int opacity) {
        int rgb = 0;
        if (hex != null && hex.matches("#[0-9A-Fa-f]{6}")) rgb = Integer.parseInt(hex.substring(1), 16);
        int a = Math.round(Math.max(0, Math.min(100, opacity)) * 255 / 100f);
        return (a << 24) | rgb;
    }
}
