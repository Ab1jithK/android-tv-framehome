package local.framehome;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

/**
 * White marks for the list. Bundled brand marks where one exists; otherwise the app's own logo is
 * lifted out of its launcher icon: pixels that differ from the tile's background become white.
 */
final class Marks {
    private static final int N = 128;
    private static final Map<String, Drawable> cache = new HashMap<>();

    static Drawable get(Context c, String pkg) {
        int res = Apps.mark(pkg);
        if (res != 0) return c.getDrawable(res);
        if (cache.containsKey(pkg)) return cache.get(pkg);
        Drawable d = null;
        try {
            d = extract(c, c.getPackageManager().getApplicationIcon(pkg));
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            // no mark: the row shows its name alone
        }
        cache.put(pkg, d);
        return d;
    }

    private static Drawable extract(Context c, Drawable icon) {
        Bitmap src = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888);
        icon.setBounds(0, 0, N, N);
        icon.draw(new Canvas(src));
        int[] px = new int[N * N];
        src.getPixels(px, 0, N, 0, 0, N, N);
        src.recycle();

        // Background: average of points just inside the corners.
        int[] probe = {idx(6, 6), idx(N - 7, 6), idx(6, N - 7), idx(N - 7, N - 7), idx(N / 2, 4), idx(4, N / 2)};
        int r = 0, g = 0, b = 0, a = 0;
        for (int p : probe) {
            r += Color.red(px[p]);
            g += Color.green(px[p]);
            b += Color.blue(px[p]);
            a += Color.alpha(px[p]);
        }
        int k = probe.length;
        r /= k;
        g /= k;
        b /= k;
        boolean transparentTile = a / k < 128;
        // A white tile would make everything "white": skip straight to the distance key.
        boolean whiteTile = !transparentTile && Math.min(r, Math.min(g, b)) > 200;
        if (whiteTile) return build(c, px, r, g, b, Mode.DISTANCE);

        // Most TV logos are white or silver on a coloured (often gradient) tile, so key on whiteness first.
        // Fall back to "differs from the corner colour" for flat tiles whose logo isn't white.
        if (!transparentTile) {
            Drawable white = build(c, px, 0, 0, 0, Mode.WHITENESS);
            if (white != null) return white;
        }
        return build(c, px, r, g, b, transparentTile ? Mode.ALPHA : Mode.DISTANCE);
    }

    private enum Mode { ALPHA, WHITENESS, DISTANCE }

    private static Drawable build(Context c, int[] px, int r, int g, int b, Mode mode) {
        boolean transparentTile = mode == Mode.ALPHA;
        int[] out = new int[N * N];
        int minX = N, minY = N, maxX = -1, maxY = -1;
        int solid = 0;
        for (int y = 0; y < N; y++) {
            for (int x = 0; x < N; x++) {
                int p = px[idx(x, y)];
                int alpha;
                if (mode == Mode.ALPHA) {
                    alpha = Color.alpha(p);
                } else if (mode == Mode.WHITENESS) {
                    int hi = Math.max(Color.red(p), Math.max(Color.green(p), Color.blue(p)));
                    int lo = Math.min(Color.red(p), Math.min(Color.green(p), Color.blue(p)));
                    double w = lo - .6 * (hi - lo);
                    alpha = (int) Math.max(0, Math.min(255, (w - 105) * 255 / 80));
                    alpha = alpha * Color.alpha(p) / 255;
                } else {
                    int dr = Color.red(p) - r, dg = Color.green(p) - g, db = Color.blue(p) - b;
                    double dist = Math.sqrt(dr * dr + dg * dg + db * db);
                    alpha = (int) Math.max(0, Math.min(255, (dist - 36) * 255 / 110));
                    alpha = alpha * Color.alpha(p) / 255;
                }
                if (alpha > 128) solid++;
                out[idx(x, y)] = Color.argb(alpha, 255, 255, 255);
                if (alpha > 40) {
                    minX = Math.min(minX, x);
                    maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        if (maxX < 0) return null;
        int w = maxX - minX + 1, h = maxY - minY + 1;
        // A mark that fills the tile edge to edge means the background guess failed;
        // a few stray pixels mean there was no logo of that kind to find.
        if (!transparentTile && w > N - 6 && h > N - 6) return null;
        if (solid < N * N / 200) return null;

        Bitmap mark = Bitmap.createBitmap(N, N, Bitmap.Config.ARGB_8888);
        Bitmap logo = Bitmap.createBitmap(out, N, N, Bitmap.Config.ARGB_8888);
        Bitmap cropped = Bitmap.createBitmap(logo, minX, minY, w, h);
        float scale = Math.min(N / (float) w, N / (float) h);
        int sw = Math.round(w * scale), sh = Math.round(h * scale);
        Bitmap fitted = Bitmap.createScaledBitmap(cropped, sw, sh, true);
        new Canvas(mark).drawBitmap(fitted, (N - sw) / 2f, (N - sh) / 2f, null);
        logo.recycle();
        return new BitmapDrawable(c.getResources(), mark);
    }

    private static int idx(int x, int y) {
        return y * N + x;
    }
}
