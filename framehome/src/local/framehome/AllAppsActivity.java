package local.framehome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Every installed app. OK opens it; holding OK adds it to or removes it from Home. */
public class AllAppsActivity extends Activity {
    private Stage st;
    private ListPane list;
    private List<String[]> apps;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Stage(this);
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);

        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        Bitmap bmp = Wallpapers.get(this, getResources().getDisplayMetrics().widthPixels);
        if (bmp != null) photo.setImageBitmap(bmp);
        root.addView(photo, match());
        root.addView(new ShadeView(this, st.s, .38f), match());

        TextView title = st.text(this, 48, 500, -1.5f, Stage.INK);
        title.setText("All apps");
        root.addView(title, Stage.at(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, st.px(140), st.px(78)));

        TextView hint = st.text(this, 30, 500, -.2f, 0xE0FFFFFF);
        hint.setText("Hold OK to add to or remove from Home");
        root.addView(hint, Stage.at(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, st.px(140), st.px(146)));

        list = new ListPane(this, st, 96, 60, 820, 0, 40, 200);
        root.addView(list, Stage.at(st.px(1200), st.px(820), st.px(112), st.px(220)));
        list.setListener(new ListPane.Listener() {
            @Override
            public void onOpen(int i) {
                Apps.launch(AllAppsActivity.this, apps.get(i)[0]);
            }

            @Override
            public void onHold(int i) {
                List<String> pins = Apps.pinned(AllAppsActivity.this);
                String pkg = apps.get(i)[0];
                if (!pins.remove(pkg)) pins.add(pkg);
                Apps.save(AllAppsActivity.this, pins);
                refresh();
                list.focus(i);
            }
        });
        setContentView(root);
        refresh();
        list.focus(0);
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void refresh() {
        apps = Apps.allLaunchable(this);
        List<String> pins = Apps.pinned(this);
        List<Drawable> marks = new ArrayList<>();
        List<CharSequence> names = new ArrayList<>();
        for (String[] a : apps) {
            marks.add(Marks.get(this, a[0]));
            if (pins.contains(a[0])) {
                String tag = "   On Home";
                SpannableString s = new SpannableString(a[1] + tag);
                int from = a[1].length();
                s.setSpan(new RelativeSizeSpan(.45f), from, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                s.setSpan(new ForegroundColorSpan(0xA8F4F4F1), from, s.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                names.add(s);
            } else {
                names.add(a[1]);
            }
        }
        list.setEntries(marks, names);
    }
}
