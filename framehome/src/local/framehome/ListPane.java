package local.framehome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * CleanFrame's ph-list: big type rows with a mark, faded at the top and bottom edges.
 * Unfocused rows sit at 62% opacity; the focused row is full and scaled 1.05 from its mark.
 */
final class ListPane extends FrameLayout {
    interface Listener {
        void onOpen(int index);

        void onHold(int index);
    }

    private final Stage st;
    private final float rowH;
    private final float nameSize;
    private final int lead;
    private final int maxSlot;
    private final int visible;
    private final float topFade;
    private final float bottomFade;
    private final FrameLayout rows;
    private final List<View> views = new ArrayList<>();
    private final Paint mask = new Paint();
    private Listener listener;
    private View escape;
    private int focused;

    /** All sizes in stage pixels. {@code lead} = empty row slots above the first row. */
    ListPane(Context c, Stage st, float rowH, float nameSize, float height, int lead, float topFade, float bottomFade) {
        super(c);
        this.st = st;
        this.rowH = rowH;
        this.nameSize = nameSize;
        this.lead = lead;
        this.topFade = topFade;
        this.bottomFade = bottomFade;
        visible = (int) (height / rowH);
        maxSlot = Math.max(1, visible - 3 - lead);
        mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        rows = new FrameLayout(c);
        rows.setClipChildren(false);
        setClipChildren(false);
        addView(rows, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    void setListener(Listener l) {
        listener = l;
    }

    /** Where Up from the first row and Right from any row go (the utility icons). */
    void setEscape(View v) {
        escape = v;
    }

    int focusedIndex() {
        return focused;
    }

    int size() {
        return views.size();
    }

    void setEntries(List<Drawable> marks, List<CharSequence> names) {
        rows.removeAllViews();
        views.clear();
        for (int i = 0; i < names.size(); i++) {
            View row = makeRow(marks.get(i), names.get(i), i);
            rows.addView(row, Stage.at(st.px(1200), st.px(rowH), 0, st.px((i + lead) * rowH)));
            views.add(row);
        }
        for (int i = 0; i < views.size(); i++) {
            View v = views.get(i);
            v.setNextFocusUpId(i > 0 ? views.get(i - 1).getId() : escape != null ? escape.getId() : v.getId());
            v.setNextFocusDownId(i < views.size() - 1 ? views.get(i + 1).getId() : v.getId());
            v.setNextFocusLeftId(v.getId());
            v.setNextFocusRightId(escape != null ? escape.getId() : v.getId());
        }
        focused = Math.min(focused, Math.max(0, views.size() - 1));
        scrollTo(focused, false);
    }

    void focus(int i) {
        if (views.isEmpty()) return;
        focused = Math.max(0, Math.min(i, views.size() - 1));
        views.get(focused).requestFocus();
    }

    private View makeRow(Drawable mark, CharSequence name, int index) {
        float k = rowH / 128f;
        FrameLayout row = new FrameLayout(getContext());
        row.setId(View.generateViewId());
        row.setFocusable(true);
        row.setClickable(true);
        row.setClipChildren(false);

        FrameLayout group = new FrameLayout(getContext());
        group.setClipChildren(false);
        group.setAlpha(.62f);
        group.setPivotX(st.px(28 * k));
        group.setPivotY(st.px(64 * k));
        row.addView(group, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (mark != null) {
            ImageView iv = new ImageView(getContext());
            iv.setImageDrawable(mark);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            group.addView(iv, Stage.at(st.px(64 * k), st.px(64 * k), st.px(28 * k), st.px(32 * k)));
        }
        TextView t = st.text(getContext(), nameSize, 600, -1.8f * nameSize / 88f, Stage.INK);
        t.setText(name);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxWidth(st.px(992 * k));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setShadowLayer(st.s * 12, 0, st.s * 3, 0x4A0E0A06);
        LayoutParams tp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        tp.leftMargin = st.px(128 * k);
        group.addView(t, tp);

        row.setOnFocusChangeListener((v, has) -> {
            group.animate().alpha(has ? 1f : .62f).scaleX(has ? 1.05f : 1f).scaleY(has ? 1.05f : 1f)
                    .setDuration(Stage.T).setInterpolator(Stage.EASE);
            if (has) {
                focused = index;
                scrollTo(index, true);
            }
        });
        row.setOnClickListener(v -> {
            if (listener != null) listener.onOpen(index);
        });
        row.setOnLongClickListener(v -> {
            if (listener != null) listener.onHold(index);
            return true;
        });
        return row;
    }

    private void scrollTo(int i, boolean animate) {
        // Scroll only when the list overflows, and never past its last row.
        int overflow = Math.max(0, lead + views.size() - visible);
        float y = -Math.min(Math.max(0, i - maxSlot), overflow) * rowH * st.s;
        if (animate) rows.animate().translationY(y).setDuration(Stage.T).setInterpolator(Stage.EASE);
        else rows.setTranslationY(y);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        float t = Math.min(.45f, topFade * st.s / h), b = Math.min(.45f, bottomFade * st.s / h);
        mask.setShader(new LinearGradient(0, 0, 0, h,
                new int[] {0, 0xFF000000, 0xFF000000, 0}, new float[] {0, t, 1 - b, 1}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void dispatchDraw(Canvas c) {
        int sc = c.saveLayer(0, 0, getWidth(), getHeight(), null);
        super.dispatchDraw(c);
        c.drawRect(0, 0, getWidth(), getHeight(), mask);
        c.restoreToCount(sc);
    }
}
