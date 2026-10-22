package local.framehome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Bundled CleanFrame wallpapers, plus an optional custom photo pushed to
 * /sdcard/Android/data/local.framehome/files/wallpaper.jpg. Only one bitmap is kept in memory.
 */
final class Wallpapers {
    private static final String[] BUILT_IN = {"misty", "cove", "forest", "valley", "peaks", "night"};
    private static final String CUSTOM = "custom";

    private static String cachedKey;
    private static Bitmap cached;

    private interface Opener {
        InputStream open() throws IOException;
    }

    private static File customFile(Context c) {
        File dir = c.getExternalFilesDir(null);
        return dir == null ? null : new File(dir, "wallpaper.jpg");
    }

    private static List<String> choices(Context c) {
        List<String> l = new ArrayList<>();
        File f = customFile(c);
        if (f != null && f.isFile()) l.add(CUSTOM);
        l.addAll(Arrays.asList(BUILT_IN));
        return l;
    }

    private static String current(Context c) {
        List<String> ch = choices(c);
        String v = Apps.prefs(c).getString("wallpaper", ch.get(0));
        return ch.contains(v) ? v : ch.get(0);
    }

    static void next(Context c) {
        List<String> ch = choices(c);
        String n = ch.get((ch.indexOf(current(c)) + 1) % ch.size());
        Apps.prefs(c).edit().putString("wallpaper", n).apply();
    }

    static Bitmap get(Context c, int maxWidth) {
        String name = current(c);
        File f = customFile(c);
        String key = CUSTOM.equals(name) ? CUSTOM + f.lastModified() : name;
        if (key.equals(cachedKey) && cached != null) return cached;
        Bitmap b;
        try {
            b = CUSTOM.equals(name)
                    ? decode(() -> new FileInputStream(f), maxWidth)
                    : decode(() -> c.getAssets().open("wp_" + name + ".jpg"), maxWidth);
        } catch (IOException | RuntimeException e) {
            android.util.Log.w("FrameHome", "wallpaper " + name + " failed", e);
            b = null;
        }        if (b != null) {
            cachedKey = key;
            cached = b;
        }
        return b;
    }

    private static Bitmap decode(Opener o, int maxWidth) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = o.open()) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inPreferredConfig = Bitmap.Config.RGB_565;
        opt.inSampleSize = 1;
        while (bounds.outWidth / (opt.inSampleSize * 2) >= maxWidth) opt.inSampleSize *= 2;
        Bitmap b;
        try (InputStream in = o.open()) {
            b = BitmapFactory.decodeStream(in, null, opt);
        }
        if (b != null && b.getWidth() > maxWidth) {
            Bitmap scaled = Bitmap.createScaledBitmap(b, maxWidth, Math.round(b.getHeight() * (maxWidth / (float) b.getWidth())), true);
            if (scaled != b) b.recycle();
            b = scaled;
        }
        return b;
    }
}
