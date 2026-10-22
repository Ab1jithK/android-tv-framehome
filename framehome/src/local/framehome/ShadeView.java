package local.framehome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Shader;
import android.view.View;

/** CleanFrame's photo treatment for the List layout: ph-dim, shade, ph-col and ph-scrim. */
final class ShadeView extends View {
    private final float s;
    private final float extraDim;
    private final Paint dim = new Paint();
    private final Paint shade = new Paint();
    private final Paint col = new Paint();
    private final Paint scrim = new Paint();
    private final Paint flat = new Paint();

    ShadeView(Context c, float scale, float extraDim) {
        super(c);
        s = scale;
        this.extraDim = extraDim;
        flat.setColor(ink(extraDim));
    }

    private static int ink(float a) {
        return Color.argb(Math.round(a * 255), 10, 8, 6);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        Shader.TileMode cl = Shader.TileMode.CLAMP;
        dim.setShader(new LinearGradient(0, 0, w, 0,
                new int[] {ink(.26f), ink(.16f), ink(.08f)}, new float[] {0, .5f, 1}, cl));
        shade.setShader(new LinearGradient(0, 0, 0, 300 * s,
                new int[] {ink(.32f), ink(.14f), ink(0)}, new float[] {0, .5f, 1}, cl));
        Shader colX = new LinearGradient(0, 0, 1040 * s, 0,
                new int[] {ink(.18f), ink(.18f), ink(0)}, new float[] {0, 600 / 1040f, 1}, cl);
        Shader colY = new LinearGradient(0, 160 * s, 0, 980 * s,
                new int[] {0, Color.BLACK, Color.BLACK, 0}, new float[] {0, 120 / 820f, 700 / 820f, 1}, cl);
        col.setShader(new ComposeShader(colX, colY, PorterDuff.Mode.DST_IN));
        scrim.setShader(new LinearGradient(0, 600 * s, 0, 1080 * s,
                new int[] {ink(0), ink(.26f), ink(.40f)}, new float[] {0, .55f, 1}, cl));
    }

    @Override
    protected void onDraw(Canvas c) {
        int w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, h, dim);
        c.drawRect(0, 0, w, 300 * s, shade);
        c.drawRect(0, 160 * s, 1040 * s, 980 * s, col);
        c.drawRect(0, 600 * s, w, h, scrim);
        if (extraDim > 0) c.drawRect(0, 0, w, h, flat);
    }
}
