package local.tvhome.wallfetch;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Hands the current set to Home (readPermission: signature). call("state") gives the version (time of the last
 * update) and the file names; openFile(content://local.tvhome.wallfetch.walls/<name>) opens one read-only.
 */
public final class WallsProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (!"state".equals(method)) return null;
        // call() is not covered by readPermission: check it here too.
        getContext().enforceCallingPermission("local.tvhome.wallfetch.READ_WALLS", "read wallpapers");
        Bundle b = new Bundle();
        File[] files = Store.pictures(Store.walls(getContext()));
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) names[i] = files[i].getName();
        Arrays.sort(names);
        b.putLong("version", Store.last(getContext()));
        b.putStringArray("names", names);
        return b;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (!"r".equals(mode) || name == null || !name.matches("wh-[A-Za-z0-9]{1,16}\\.jpg")) {
            throw new FileNotFoundException(uri.toString());
        }
        return ParcelFileDescriptor.open(new File(Store.walls(getContext()), name), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { return null; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
