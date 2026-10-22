package local.framehome;

import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

/** CleanFrame lays out on a 1920x1080 stage; this maps stage pixels to the TV's real pixels. */
final class Stage {
    static final int INK = 0xFFF4F4F1;
    static final int UTIL_INK = 0xFFF6F1E7;
    static final Interpolator EASE = new PathInterpolator(.2f, .7f, .2f, 1f);
    static final int T = 180;

    private static Typeface manrope;

    final float s;

    Stage(Context c) {
        DisplayMetrics m = c.getResources().getDisplayMetrics();
        s = Math.max(m.widthPixels, m.heightPixels) / 1920f;
    }

    int px(float stagePx) {
        return Math.round(stagePx * s);
    }

    static Typeface font(Context c) {
        if (manrope == null) {
            try {
                manrope = Typeface.createFromAsset(c.getAssets(), "Manrope.ttf");
            } catch (RuntimeException e) {
                manrope = Typeface.SANS_SERIF;
            }
        }
        return manrope;
    }

    TextView text(Context c, float sizePx, int weight, float trackingPx, int color) {
        TextView t = new TextView(c);
        t.setTypeface(font(c));
        t.setFontVariationSettings("'wght' " + weight);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * s);
        t.setLetterSpacing(trackingPx / sizePx);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setIncludeFontPadding(false);
        return t;
    }

    static FrameLayout.LayoutParams at(int w, int h, int left, int top) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(w, h);
        p.gravity = Gravity.TOP | Gravity.START;
        p.leftMargin = left;
        p.topMargin = top;
        return p;
    }
}
