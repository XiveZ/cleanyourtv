package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Takes the pictures the New wallpapers add-on (addons/wallpaper-fetch) downloaded into the theme's wallpapers/
 * folder. The launcher itself stays without network: the add-on downloads, this only copies from its provider
 * (signature permission). Runs off the main thread when Home comes back; the theme's other files stay.
 */
final class WallpaperImport {
    private WallpaperImport() { }

    static final String AUTHORITY = "local.tvhome.wallfetch.walls";
    private static final String IMPORTED_KEY = "imported_walls";
    private static final AtomicBoolean RUNNING = new AtomicBoolean();

    /** A name the add-on may hand over: one of its own pictures, never a path. */
    static boolean safeName(String name) {
        return name != null && name.matches("wh-[A-Za-z0-9]{1,16}\\.jpg");
    }

    static List<String> safeNames(String[] names) {
        List<String> out = new ArrayList<>();
        if (names != null) for (String n : names) if (safeName(n)) out.add(n);
        return out;
    }

    static boolean shouldImport(long version, long imported, String[] names) {
        return version > imported && !safeNames(names).isEmpty();
    }

    /** Starts a check in the background; {@code changed} runs on that thread when Home's set was replaced. */
    static void syncAsync(Context ctx, Runnable changed) {
        Context app = ctx.getApplicationContext();
        if (!RUNNING.compareAndSet(false, true)) return;
        new Thread(() -> {
            try {
                if (sync(app)) changed.run();
            } finally {
                RUNNING.set(false);
            }
        }, "wallpaper-import").start();
    }

    /** True when a newer set was copied in. Any problem leaves the current wallpapers as they are. */
    static boolean sync(Context ctx) {
        ContentResolver resolver = ctx.getContentResolver();
        Uri base = Uri.parse("content://" + AUTHORITY);
        Bundle state;
        try {
            state = resolver.call(base, "state", null, null);
        } catch (RuntimeException e) {
            return false;   // the add-on is not installed, or not ours
        }
        if (state == null) return false;
        SharedPreferences prefs = ctx.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        long version = state.getLong("version", 0);
        String[] names = state.getStringArray("names");
        if (!shouldImport(version, prefs.getLong(IMPORTED_KEY, 0), names)) return false;
        File dir = Config.dir(ctx);
        if (dir == null) return false;
        File walls = new File(dir, "wallpapers"), incoming = new File(dir, "wallpapers.import");
        clear(incoming);
        if (!incoming.mkdirs()) return false;
        try {
            for (String name : safeNames(names)) {
                try (InputStream in = resolver.openInputStream(Uri.withAppendedPath(base, name));
                     OutputStream out = new FileOutputStream(new File(incoming, name))) {
                    if (in == null) throw new java.io.IOException("no " + name);
                    byte[] buf = new byte[64 * 1024];
                    for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
                }
            }
        } catch (Exception e) {
            clear(incoming);
            return false;
        }
        // The new set is complete: only now do the old pictures go (captions and other theme files stay).
        if (!walls.isDirectory() && !walls.mkdirs()) return false;
        File[] old = walls.listFiles();
        if (old != null) for (File f : old) if (isPicture(f.getName())) f.delete();
        File[] fresh = incoming.listFiles();
        if (fresh != null) for (File f : fresh) f.renameTo(new File(walls, f.getName()));
        clear(incoming);
        prefs.edit().putLong(IMPORTED_KEY, version)
                .putLong(Config.SETTINGS_REVISION_KEY, prefs.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
        return true;
    }

    private static boolean isPicture(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp");
    }

    private static void clear(File dir) {
        File[] files = dir.listFiles();
        if (files != null) for (File f : files) f.delete();
        dir.delete();
    }
}
