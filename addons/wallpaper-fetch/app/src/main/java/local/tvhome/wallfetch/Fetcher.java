package local.tvhome.wallfetch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Downloads a new set of pictures into incoming/, then swaps it in for walls/ in one step: Home never sees half a
 * set, and a crash or a lost network leaves the old set as it was (the next run starts incoming/ again).
 */
final class Fetcher {
    private static final String TAG = "WallFetch";
    private static final int W = 1920, H = 1080;
    private static final int MAX_BYTES = 25 * 1024 * 1024;

    interface Progress {
        void at(int got, int of);
    }

    /** The number of pictures now in walls/; throws when nothing could be downloaded. */
    static int run(Context ctx, int count, Progress progress) throws IOException, InterruptedException {
        File in = Store.incoming(ctx);
        clear(in);
        if (!in.mkdirs() && !in.isDirectory()) throw new IOException("cannot make " + in);
        Set<String> seen = new HashSet<>();
        int got = 0;
        List<String[]> pairs = Wallhaven.pairs();
        int perPair = Math.max(1, count / pairs.size() + 1);
        for (int page = 1; got < count && page <= 20; page++) {
            boolean found = false;
            for (String[] pair : pairs) {
                if (got >= count) break;
                Thread.sleep(1500);   // wallhaven allows 45 requests a minute
                List<Wallhaven.Item> items;
                try {
                    items = Wallhaven.parse(new String(get(Wallhaven.searchUrl(pair[0], pair[1], page)), "UTF-8"));
                } catch (org.json.JSONException e) {
                    continue;
                }
                int taken = 0;
                for (Wallhaven.Item item : items) {
                    if (got >= count || taken >= perPair) break;
                    if (!seen.add(item.id)) continue;
                    found = true;
                    try {
                        save(get(item.url), new File(in, "wh-" + item.id + ".jpg"));
                    } catch (IOException e) {
                        Log.w(TAG, "skipping " + item.id + ": " + e.getMessage());
                        continue;
                    }
                    got++;
                    taken++;
                    progress.at(got, count);
                }
            }
            if (!found) break;
        }
        if (got == 0) throw new IOException("no pictures downloaded");
        swapIn(ctx, in);
        return got;
    }

    /** incoming/ becomes walls/; the old set is removed afterwards. */
    private static void swapIn(Context ctx, File in) throws IOException {
        File walls = Store.walls(ctx), old = new File(ctx.getFilesDir(), "walls.old");
        clear(old);
        if (walls.exists() && !walls.renameTo(old)) throw new IOException("cannot move the old set");
        if (!in.renameTo(walls)) {
            old.renameTo(walls);
            throw new IOException("cannot move the new set");
        }
        clear(old);
    }

    private static void clear(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) f.delete();
        dir.delete();
    }

    private static byte[] get(String url) throws IOException, InterruptedException {
        IOException last = null;
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(30_000);
            c.setReadTimeout(30_000);
            c.setRequestProperty("User-Agent", "cleanyourtv-wallpaper-fetch/1.0");
            try {
                int code = c.getResponseCode();
                if (code == 200) return read(c.getInputStream());
                last = new IOException("HTTP " + code);
            } catch (IOException e) {
                last = e;
            } finally {
                c.disconnect();
            }
            Thread.sleep(5000L * (attempt + 1));   // 429 and short outages
        }
        throw last;
    }

    private static byte[] read(InputStream in) throws IOException {
        try (InputStream s = in) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64 * 1024];
            for (int n; (n = s.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                if (out.size() > MAX_BYTES) throw new IOException("too large");
            }
            return out.toByteArray();
        }
    }

    /** Decodes small enough, cuts to exactly 1920x1080 (Cover) and writes a JPEG. */
    private static void save(byte[] data, File file) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("not a picture");
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Cover.sampleSize(bounds.outWidth, bounds.outHeight, W, H);
        Bitmap src = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        if (src == null) throw new IOException("cannot decode");
        Bitmap out = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        try {
            Cover cover = Cover.of(src.getWidth(), src.getHeight(), W, H);
            Matrix m = new Matrix();
            m.setScale(cover.scale, cover.scale);
            m.postTranslate(-cover.left, -cover.top);
            new Canvas(out).drawBitmap(src, m, new Paint(Paint.FILTER_BITMAP_FLAG));
            try (FileOutputStream f = new FileOutputStream(file)) {
                if (!out.compress(Bitmap.CompressFormat.JPEG, 85, f)) throw new IOException("cannot write");
            }
        } finally {
            src.recycle();
            out.recycle();
        }
    }
}
