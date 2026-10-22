package local.framehome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Home: photo, clock, a list of your apps, and quiet utility icons top right. */
public class HomeActivity extends Activity {
    private Stage st;
    private ImageView photo;
    private TextView clock;
    private TextView utilLabel;
    private ListPane list;
    private final List<View> utils = new ArrayList<>();
    private final List<String> shown = new ArrayList<>();

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateClock();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        st = new Stage(this);
        FrameLayout root = new FrameLayout(this);
        root.setClipChildren(false);

        photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(photo, match());
        root.addView(new ShadeView(this, st.s, 0), match());

        clock = st.text(this, 48, 500, -1.5f, Stage.INK);
        clock.setFontFeatureSettings("tnum");
        clock.setGravity(Gravity.CENTER_VERTICAL);
        clock.setShadowLayer(st.s * 3, 0, st.s, 0x38000000);
        root.addView(clock, Stage.at(ViewGroup.LayoutParams.WRAP_CONTENT, st.px(64), st.px(140), st.px(72)));

        list = new ListPane(this, st, 128, 88, 944, 1, 128, 220);
        root.addView(list, Stage.at(st.px(1200), st.px(944), st.px(112), st.px(72)));

        LinearLayout bar = new LinearLayout(this);
        FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, st.px(64));
        bp.gravity = Gravity.TOP | Gravity.END;
        bp.rightMargin = st.px(122);
        bp.topMargin = st.px(72);
        root.addView(bar, bp);
        addUtil(bar, R.drawable.ic_inputs, "Inputs", this::openInputs);
        addUtil(bar, R.drawable.ic_art, "Wallpaper", () -> {
            Wallpapers.next(this);
            applyWallpaper();
        });
        addUtil(bar, R.drawable.ic_apps, "All apps", () -> startActivity(new Intent(this, AllAppsActivity.class)));
        addUtil(bar, R.drawable.ic_settings, "Settings", () -> startSafely(new Intent(Settings.ACTION_SETTINGS)));

        utilLabel = st.text(this, 28, 500, -.2f, Stage.UTIL_INK);
        utilLabel.setShadowLayer(st.s * 8, 0, st.s * 2, 0x3D0E0A06);
        utilLabel.setVisibility(View.INVISIBLE);
        root.addView(utilLabel, Stage.at(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 0, st.px(140)));

        list.setEscape(utils.get(0));
        list.setListener(new ListPane.Listener() {
            @Override
            public void onOpen(int i) {
                if (shown.isEmpty()) startActivity(new Intent(HomeActivity.this, AllAppsActivity.class));
                else Apps.launch(HomeActivity.this, shown.get(i));
            }

            @Override
            public void onHold(int i) {
                if (!shown.isEmpty()) edit(i);
            }
        });
        setContentView(root);
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void addUtil(LinearLayout bar, int icon, String name, Runnable action) {
        ImageView v = new ImageView(this);
        v.setId(View.generateViewId());
        v.setImageResource(icon);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int pad = st.px(14);
        v.setPadding(pad, pad, pad, pad);
        v.setFocusable(true);
        v.setClickable(true);
        v.setAlpha(.66f);
        v.setContentDescription(name);
        v.setOnClickListener(x -> action.run());
        v.setOnFocusChangeListener((x, has) -> {
            x.animate().alpha(has ? 1f : .66f).scaleX(has ? 1.1f : 1f).scaleY(has ? 1.1f : 1f)
                    .setDuration(140).setInterpolator(Stage.EASE);
            if (has) showUtilLabel(x, name);
            else utilLabel.setVisibility(View.INVISIBLE);
        });
        boolean first = utils.isEmpty();
        v.setOnKeyListener((x, code, e) -> {
            if (e.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (code == KeyEvent.KEYCODE_DPAD_DOWN || (first && code == KeyEvent.KEYCODE_DPAD_LEFT)) {
                list.focus(list.focusedIndex());
                return true;
            }
            return false;
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(st.px(64), st.px(64));
        if (!first) lp.leftMargin = st.px(16);
        bar.addView(v, lp);
        utils.add(v);
    }

    private void showUtilLabel(View icon, String name) {
        utilLabel.setText(name);
        utilLabel.setVisibility(View.VISIBLE);
        utilLabel.post(() -> {
            View bar = (View) icon.getParent();
            float center = bar.getX() + icon.getX() + icon.getWidth() / 2f;
            float x = center - utilLabel.getWidth() / 2f;
            float maxX = ((View) bar.getParent()).getWidth() - st.px(122) - utilLabel.getWidth();
            utilLabel.setX(Math.min(x, maxX));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(tick, f);
        updateClock();
        applyWallpaper();
        refreshList();
        View cur = getCurrentFocus();
        if (cur == null || !utils.contains(cur)) list.focus(list.focusedIndex());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(tick);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        list.focus(0);
    }

    @Override
    public void onBackPressed() {
        list.focus(0);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        View cur = getCurrentFocus();
        if (code == KeyEvent.KEYCODE_MENU && cur != null && !utils.contains(cur) && !shown.isEmpty()) {
            edit(list.focusedIndex());
            return true;
        }
        return super.onKeyDown(code, e);
    }

    private void updateClock() {
        String fmt = DateFormat.is24HourFormat(this) ? "H:mm" : "h:mm";
        clock.setText(DateFormat.format(fmt, new Date()));
    }

    private void applyWallpaper() {
        Bitmap b = Wallpapers.get(this, getResources().getDisplayMetrics().widthPixels);
        if (b != null) photo.setImageBitmap(b);
        else photo.setImageDrawable(new android.graphics.drawable.ColorDrawable(0xFF1D2A30));
    }

    private void refreshList() {
        PackageManager pm = getPackageManager();
        shown.clear();
        List<Drawable> marks = new ArrayList<>();
        List<CharSequence> names = new ArrayList<>();
        for (String pkg : Apps.pinned(this)) {
            String label = Apps.label(pm, pkg);
            if (label == null || Apps.launchIntent(pm, pkg) == null) continue;
            shown.add(pkg);
            marks.add(Marks.get(this, pkg));
            names.add(label);
        }
        if (shown.isEmpty()) {
            marks.add(null);
            names.add("Add apps");
        }
        list.setEntries(marks, names);
    }

    private void edit(int i) {
        String pkg = shown.get(i);
        new AlertDialog.Builder(this)
                .setTitle(Apps.label(getPackageManager(), pkg))
                .setItems(new String[] {"Move up", "Move down", "Remove from Home"}, (d, which) -> {
                    List<String> pins = Apps.pinned(this);
                    int at = pins.indexOf(pkg);
                    int target = i;
                    if (which == 2) {
                        pins.remove(pkg);
                    } else {
                        // Swap with the neighbouring visible app, skipping pins that are disabled or uninstalled.
                        int j = which == 0 ? i - 1 : i + 1;
                        if (at < 0 || j < 0 || j >= shown.size()) return;
                        int other = pins.indexOf(shown.get(j));
                        pins.set(at, shown.get(j));
                        pins.set(other, pkg);
                        target = j;
                    }
                    Apps.save(this, pins);
                    refreshList();
                    list.focus(target);
                })
                .show();
    }

    private void openInputs() {
        if (startSafely(new Intent("com.android.tv.action.VIEW_INPUTS"))) return;
        Intent live = Apps.launchIntent(getPackageManager(), "com.mediatek.wwtv.tvcenter");
        if (live == null || !startSafely(live)) Toast.makeText(this, "No input picker on this TV", Toast.LENGTH_SHORT).show();
    }

    private boolean startSafely(Intent i) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
