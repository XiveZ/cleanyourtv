package local.tvhome.wallfetch;

/** How a picture is scaled and cut to cover the screen exactly (centre crop), and how small it can be decoded. */
final class Cover {
    final float scale;
    final int left, top;

    private Cover(float scale, int left, int top) {
        this.scale = scale;
        this.left = left;
        this.top = top;
    }

    static Cover of(int srcW, int srcH, int w, int h) {
        float scale = Math.max(w / (float) srcW, h / (float) srcH);
        return new Cover(scale, Math.round((srcW * scale - w) / 2f), Math.round((srcH * scale - h) / 2f));
    }

    /** The largest power of two that still leaves the decoded picture at least screen-sized. */
    static int sampleSize(int srcW, int srcH, int w, int h) {
        int n = 1;
        while (srcW / (n * 2) >= w && srcH / (n * 2) >= h) n *= 2;
        return n;
    }
}
