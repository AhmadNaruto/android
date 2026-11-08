package me.champdev.weblock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** OpenSubtitles through Stremio's public addon (no key, no login): subtitle files for an IMDb title or episode. */
final class Subs {
    static final String BASE = "https://opensubtitles-v3.strem.io/subtitles/";

    static final class Sub {
        String url, lang, release;
    }

    /** How far (dp) subtitles sit above the default spot; negative = lower. Kept across videos. */
    static int offset() {
        return App.prefs.getInt("subOffset", 0);
    }

    static String offsetLabel() {
        int o = offset();
        return o == 0 ? App.t(R.string.subs_default) : App.t(o > 0 ? R.string.subs_up : R.string.subs_down, Math.abs(o));
    }

    /** Subtitle text size in percent of the default (22 sp). */
    static int size() {
        return App.prefs.getInt("subSize", 100);
    }

    static String sizeLabel() {
        return size() == 100 ? App.t(R.string.subs_default) : size() + "%";
    }

    static final int[] COLORS = {0xFFFFFFFF, 0xFFFFE14D, 0xFF7FE7FF, 0xFF8CFF8C, 0xFFFFB3D1, 0xFFFFA64D};
    static final int[] COLOR_NAMES = {R.string.subs_white, R.string.subs_yellow, R.string.subs_cyan, R.string.subs_green, R.string.subs_pink, R.string.subs_orange};

    /** Subtitle text color, an index into COLORS. */
    static int colorIndex() {
        return Math.floorMod(App.prefs.getInt("subColor", 0), COLORS.length);
    }

    static String colorName() {
        return App.t(COLOR_NAMES[colorIndex()]);
    }

    static int color() {
        return COLORS[colorIndex()];
    }

    /** The box behind subtitles, percent opacity: 0 = none (the text keeps its shadow). */
    static int background() {
        return App.prefs.getInt("subBg", 50);
    }

    static String backgroundLabel() {
        return background() == 0 ? App.t(R.string.player_off) : background() + "%";
    }

    /** Movies and episodes start with subtitles in your language (stream's own, else OpenSubtitles). Off by default. */
    static boolean auto() {
        return App.prefs.getBoolean("subAuto", false);
    }

    /** ISO 639-2 code of the last OpenSubtitles language picked, else the device's language. */
    static String preferred() {
        return App.prefs.getString("subLang", Locale.getDefault().getISO3Language());
    }

    /** The file picked for a movie or episode (`key` from the player), null if none: it comes back by itself next time. */
    static Sub remembered(String key) {
        try {
            JSONObject o = new JSONObject(App.prefs.getString("subPicks", "{}")).optJSONObject(key);
            if (o == null) return null;
            Sub s = new Sub();
            s.url = o.getString("url");
            s.lang = o.optString("lang", "und");
            s.release = o.optString("release");
            return s;
        } catch (org.json.JSONException e) {
            return null;
        }
    }

    /** Keeps the pick for `key` (null forgets it) and makes its language the preferred one. The newest 300 are kept. */
    static void remember(String key, Sub s) {
        try {
            JSONObject all = new JSONObject(App.prefs.getString("subPicks", "{}"));
            all.remove(key);
            if (s != null) all.put(key, new JSONObject().put("url", s.url).put("lang", s.lang).put("release", s.release));
            // Android's JSONObject keeps insertion order: the first keys are the oldest
            while (all.length() > 300) all.remove(all.keys().next());
            android.content.SharedPreferences.Editor e = App.prefs.edit().putString("subPicks", all.toString());
            if (s != null) e.putString("subLang", s.lang);
            e.apply();
        } catch (org.json.JSONException ignored) { }
    }

    /** Subtitle timing for the video playing now, in ms (not saved): positive shows them later, negative earlier. Read on the player's thread. */
    static volatile long delayMs;

    static String delayLabel() {
        return delayMs == 0 ? App.t(R.string.subs_in_sync) : String.format(Locale.ROOT, "%+.1f s", delayMs / 1000f);
    }

    private static void bump(String key, int def, int by, int min, int max) {
        App.prefs.edit().putInt(key, Math.max(min, Math.min(max, App.prefs.getInt(key, def) + by))).apply();
    }

    /**
     * One panel for how subtitles look: Size, Position and (in the player) Timing, each with − and + buttons, and Reset.
     * Every press applies at once; `changed` runs after each, `closed` when the panel goes away. It sits at the top so the subtitles stay in view.
     */
    static void panel(android.app.Activity a, boolean timing, Runnable changed, Runnable closed) {
        android.app.Dialog d = new android.app.Dialog(a);
        d.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        android.widget.LinearLayout box = new android.widget.LinearLayout(a);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = Home.dp(a, 20);
        box.setPadding(pad, pad, pad, Home.dp(a, 12));
        box.setBackground(Home.rounded(0xF0181320, Home.dp(a, 18)));
        android.widget.TextView title = new android.widget.TextView(a);
        title.setText(App.t(R.string.player_subtitles));
        title.setTextColor(Home.TEXT);
        title.setTextSize(20);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        title.setPadding(0, 0, 0, Home.dp(a, 8));
        box.addView(title);
        // Title and the Reset / Done buttons stay put; the rows between them scroll when the screen is short (a phone in landscape)
        android.widget.LinearLayout body = new android.widget.LinearLayout(a);
        body.setOrientation(android.widget.LinearLayout.VERTICAL);
        android.widget.ScrollView scroll = new android.widget.ScrollView(a);
        scroll.setFillViewport(false);
        scroll.addView(body);
        box.addView(scroll, new android.widget.LinearLayout.LayoutParams(-1, 0, 1));
        List<Runnable> refresh = new ArrayList<>();
        Runnable after = () -> { for (Runnable r : refresh) r.run(); changed.run(); };
        row(a, body, refresh, App.t(R.string.subs_size), Subs::sizeLabel, by -> bump("subSize", 100, 10 * by, 50, 250), after);
        row(a, body, refresh, App.t(R.string.subs_position), Subs::offsetLabel, by -> bump("subOffset", 0, 10 * by, -100, 200), after);
        row(a, body, refresh, App.t(R.string.subs_color), Subs::colorName, by -> App.prefs.edit().putInt("subColor", Math.floorMod(colorIndex() + by, COLORS.length)).apply(), after);
        row(a, body, refresh, App.t(R.string.subs_background), Subs::backgroundLabel, by -> bump("subBg", 50, 25 * by, 0, 100), after);
        if (timing) row(a, body, refresh, App.t(R.string.subs_timing), Subs::delayLabel, by -> delayMs = Math.max(-30000, Math.min(30000, delayMs + 100L * by)), after);
        android.widget.TextView help = new android.widget.TextView(a);
        help.setText(App.t(timing ? R.string.subs_help_player : R.string.subs_help_settings));
        help.setTextColor(Home.MUTED);
        help.setTextSize(13);
        help.setPadding(0, Home.dp(a, 6), 0, Home.dp(a, 4));
        body.addView(help);
        android.widget.LinearLayout foot = new android.widget.LinearLayout(a);
        foot.setGravity(android.view.Gravity.END);
        foot.addView(button(a, App.t(R.string.subs_reset), 0, v -> {
            App.prefs.edit().remove("subSize").remove("subOffset").remove("subBg").remove("subColor").apply();
            if (timing) delayMs = 0;
            after.run();
        }));
        foot.addView(button(a, App.t(R.string.subs_done), 0, v -> d.dismiss()));
        box.addView(foot);
        d.setContentView(box);
        android.view.Window w = d.getWindow();
        w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
        w.setGravity(android.view.Gravity.TOP);
        w.setDimAmount(0.15f);
        // As tall as it needs, but never past the screen (then the rows scroll)
        int fullH = a.getResources().getDisplayMetrics().heightPixels - Home.dp(a, 32);
        box.measure(android.view.View.MeasureSpec.makeMeasureSpec(Math.min(a.getResources().getDisplayMetrics().widthPixels - Home.dp(a, 24), Home.dp(a, 560)), android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.UNSPECIFIED);
        w.setLayout(Math.min(a.getResources().getDisplayMetrics().widthPixels - Home.dp(a, 24), Home.dp(a, 560)), Math.min(fullH, box.getMeasuredHeight()));
        w.getAttributes().y = Home.dp(a, 16);
        d.setOnDismissListener(x -> closed.run());
        d.show();
        body.findViewWithTag("plus").requestFocus();   // the remote starts on Size +
    }

    /** Label, − button, value, + button. `step` gets −1 or +1. */
    private static void row(android.app.Activity a, android.widget.LinearLayout box, List<Runnable> refresh, String label,
                            java.util.function.Supplier<String> value, java.util.function.IntConsumer step, Runnable after) {
        android.widget.LinearLayout r = new android.widget.LinearLayout(a);
        r.setGravity(android.view.Gravity.CENTER_VERTICAL);
        r.setPadding(0, Home.dp(a, 4), 0, Home.dp(a, 4));
        android.widget.TextView name = new android.widget.TextView(a);
        name.setText(label);
        name.setTextColor(Home.TEXT);
        name.setTextSize(17);
        r.addView(name, new android.widget.LinearLayout.LayoutParams(0, -2, 1));
        android.widget.TextView now = new android.widget.TextView(a);
        now.setTextColor(Home.TEXT);
        now.setTextSize(17);
        now.setGravity(android.view.Gravity.CENTER);
        now.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        refresh.add(() -> now.setText(value.get()));
        now.setText(value.get());
        r.addView(button(a, "\u2212", Home.dp(a, 52), v -> { step.accept(-1); after.run(); }));
        r.addView(now, new android.widget.LinearLayout.LayoutParams(Home.dp(a, 130), -2));
        android.widget.TextView plus = button(a, "+", Home.dp(a, 52), v -> { step.accept(1); after.run(); });
        plus.setTag("plus");
        r.addView(plus);
        box.addView(r);
    }

    /** A rounded button with the accent ring when focused (remote) or pressed; `size` 0 = as wide as its text. */
    private static android.widget.TextView button(android.app.Activity a, String text, int size, android.view.View.OnClickListener click) {
        android.widget.TextView b = new android.widget.TextView(a);
        b.setText(text);
        b.setTextColor(Home.TEXT);
        b.setTextSize(size > 0 ? 24 : 16);
        b.setGravity(android.view.Gravity.CENTER);
        b.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        int r = Home.dp(a, 26);
        android.graphics.drawable.GradientDrawable on = Home.rounded(0x55FF3B5C, r);
        on.setStroke(Home.dp(a, 3), Home.ACCENT);
        android.graphics.drawable.StateListDrawable bg = new android.graphics.drawable.StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, on);
        bg.addState(new int[]{android.R.attr.state_pressed}, on);
        bg.addState(new int[]{}, Home.rounded(0x26FFFFFF, r));
        b.setBackground(bg);
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnClickListener(click);
        android.widget.LinearLayout.LayoutParams lp = size > 0 ? new android.widget.LinearLayout.LayoutParams(size, size) : new android.widget.LinearLayout.LayoutParams(-2, Home.dp(a, 44));
        lp.setMargins(Home.dp(a, 6), 0, Home.dp(a, 6), 0);
        if (size == 0) b.setPadding(Home.dp(a, 20), 0, Home.dp(a, 20), 0);
        b.setLayoutParams(lp);
        return b;
    }

    /** season 0 = a movie. Null when OpenSubtitles can't be reached. Call off the main thread. */
    static List<Sub> find(String imdb, int season, int episode) {
        String path = season > 0 ? "series/" + imdb + ":" + season + ":" + episode : "movie/" + imdb;
        try {
            JSONArray a = new JSONObject(Sites.readAll(Warp.open(BASE + path + ".json").getInputStream())).getJSONArray("subtitles");
            List<Sub> out = new ArrayList<>();
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Sub s = new Sub();
                s.url = o.getString("url");
                s.lang = o.optString("lang", "und");
                // The file name tells releases apart (the movie name is the same on every row)
                s.release = o.optString("subtitleFileName", "").isEmpty() ? o.optString("movieReleaseName", App.t(R.string.subs_file_n, i + 1)) : o.optString("subtitleFileName");
                out.add(s);
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    /** IMDb id of the best Cinemeta match for a page's title, "" when nothing matches. */
    static String imdbFor(String title, boolean series) {
        List<Catalog.Title> hit = Catalog.list(series ? "series" : "movie", "search=" + android.net.Uri.encode(title));
        return hit == null || hit.isEmpty() ? "" : hit.get(0).id;
    }

    /** Languages with their files: the one you last picked, your device's, English, then the biggest. Keys are ISO 639-2 codes. */
    static Map<String, List<Sub>> byLanguage(List<Sub> all) {
        Map<String, List<Sub>> m = new LinkedHashMap<>();
        for (Sub s : all) m.computeIfAbsent(s.lang, k -> new ArrayList<>()).add(s);
        List<Map.Entry<String, List<Sub>>> es = new ArrayList<>(m.entrySet());
        String mine = Locale.getDefault().getISO3Language(), last = preferred();
        es.sort((a, b) -> rank(b, mine, last) - rank(a, mine, last));
        Map<String, List<Sub>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Sub>> e : es) out.put(e.getKey(), e.getValue());
        return out;
    }

    private static int rank(Map.Entry<String, List<Sub>> e, String mine, String last) {
        return (e.getKey().equals(last) ? 3000 : e.getKey().equals(mine) ? 2000 : e.getKey().equals("eng") ? 1000 : 0) + e.getValue().size();
    }

    // OpenSubtitles' own codes that are not ISO languages
    static final Map<String, String> ODD = new LinkedHashMap<>();
    static {
        ODD.put("pob", "Portuguese (Brazil)"); ODD.put("zht", "Chinese (Traditional)"); ODD.put("zhc", "Chinese (Cantonese)");
        ODD.put("zhe", "Chinese (bilingual)"); ODD.put("spl", "Spanish (Latin America)"); ODD.put("spn", "Spanish (Spain)");
        ODD.put("ex", "Extremaduran"); ODD.put("ze", "Chinese (bilingual)"); ODD.put("pom", "Portuguese");
    }

    static String language(String iso3) {
        if (ODD.containsKey(iso3)) return ODD.get(iso3);
        String n = new Locale(iso3).getDisplayLanguage();
        return n.isEmpty() || n.equals(iso3) ? iso3.toUpperCase(Locale.ROOT) : n;
    }
}
