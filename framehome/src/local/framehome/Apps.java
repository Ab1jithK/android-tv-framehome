package local.framehome;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The Home list (pinned packages, in order) and app lookups. */
final class Apps {
    private static final String KEY = "pinned";
    private static final String DEFAULT = "com.netflix.ninja,com.google.android.youtube.tv,"
            + "com.amazon.amazonvideo.livingroom,in.startv.hotstar,com.sonyliv,org.videolan.vlc";

    static List<String> pinned(Context c) {
        List<String> l = new ArrayList<>();
        for (String p : prefs(c).getString(KEY, DEFAULT).split(",")) {
            if (!p.isEmpty()) l.add(p);
        }
        return l;
    }

    static void save(Context c, List<String> l) {
        prefs(c).edit().putString(KEY, TextUtils.join(",", l)).apply();
    }

    static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("home", Context.MODE_PRIVATE);
    }

    /** Monochrome brand mark, or 0 when there is no open-licensed one (CleanFrame: no fabricated monograms). */
    static int mark(String pkg) {
        switch (pkg) {
            case "com.netflix.ninja": return R.drawable.mark_netflix;
            case "com.google.android.youtube.tv": return R.drawable.mark_youtube;
            case "com.amazon.amazonvideo.livingroom": return R.drawable.mark_prime;
            case "org.videolan.vlc": return R.drawable.mark_vlc;
            default: return 0;
        }
    }

    static Intent launchIntent(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i;
    }

    static String label(PackageManager pm, String pkg) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            if (!ai.enabled) return null;
            return pm.getApplicationLabel(ai).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    static void launch(Context c, String pkg) {
        Intent i = launchIntent(c.getPackageManager(), pkg);
        if (i == null) {
            Toast.makeText(c, "Can't open this app", Toast.LENGTH_SHORT).show();
            return;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            c.startActivity(i);
        } catch (RuntimeException e) {
            Toast.makeText(c, "Can't open this app", Toast.LENGTH_SHORT).show();
        }
    }

    /** Every launchable app except this one, as {package, label}, sorted by label. */
    static List<String[]> allLaunchable(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, String> found = new LinkedHashMap<>();
        for (String cat : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(cat);
            for (ResolveInfo ri : pm.queryIntentActivities(q, 0)) {
                String p = ri.activityInfo.packageName;
                if (p.equals(c.getPackageName()) || found.containsKey(p)) continue;
                found.put(p, ri.activityInfo.applicationInfo.loadLabel(pm).toString());
            }
        }
        List<String[]> l = new ArrayList<>();
        for (Map.Entry<String, String> e : found.entrySet()) l.add(new String[] {e.getKey(), e.getValue()});
        Collections.sort(l, (a, b) -> a[1].compareToIgnoreCase(b[1]));
        return l;
    }
}
