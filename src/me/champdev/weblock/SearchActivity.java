package me.champdev.weblock;

import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Type to find a site (by name, tag, language or row) or something you watched. */
public class SearchActivity extends Home {
    static final int MAX_SITES = 120;
    // Its own threads: the shared pool is often busy with poster downloads, and the answer would wait behind them
    static final ExecutorService lane = Executors.newCachedThreadPool();
    EditText box;
    LinearLayout results;
    String query = "";
    boolean keyboardShown;
    final Runnable render = this::showResults;
    final Runnable fetchTitles = this::searchTitles;
    List<Catalog.Title> titles = new ArrayList<>();
    String titlesFor = "";
    boolean titlesFailed;   // the catalog could not be reached for titlesFor

    @Override boolean tabs() { return false; }

    @Override void build() {
        col.removeAllViews();
        firstCard = null;
        box = WebActivity.field(this, "Search movies, series, sites or languages", InputType.TYPE_CLASS_TEXT);
        box.setSingleLine(true);
        box.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        box.setTextColor(TEXT);
        box.setHintTextColor(MUTED);
        box.setTextSize(sp(20, 17));
        box.setBackground(rounded(CARD, dp(14)));
        box.setPadding(u(18, 14), u(14, 10), u(18, 14), u(14, 10));
        iconize(box, R.drawable.ic_search, MUTED);
        box.setTextColor(TEXT);
        focusable(box, dp(14));
        box.setText(query);
        box.setSelection(query.length());
        box.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) {
                query = e.toString();
                App.main.removeCallbacks(render);
                App.main.postDelayed(render, 150);
                App.main.removeCallbacks(fetchTitles);
                App.main.postDelayed(fetchTitles, 300);
            }
        });
        // "Search" on the keyboard: hide it so the arrows can move over the results
        box.setOnEditorActionListener((v, action, ev) -> {
            ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(box.getWindowToken(), 0);
            if (firstCard != null) firstCard.requestFocus();
            return true;
        });
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.addView(box, new LinearLayout.LayoutParams(0, -2, 1));
        android.widget.ImageView mic = iconButton(R.drawable.ic_mic, "Voice search", v -> listen(box::setText));
        ((LinearLayout.LayoutParams) mic.getLayoutParams()).setMargins(dp(10), 0, 0, 0);
        mic.setColorFilter(TEXT);
        bar.addView(mic);
        col.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        col.addView(results, new LinearLayout.LayoutParams(-1, -2));
        showResults();

        // Keyboard up on the first visit only, not every time you come back from a site
        if (!keyboardShown) {
            keyboardShown = true;
            box.requestFocus();
            if (getIntent().getBooleanExtra("voice", false)) box.post(() -> listen(box::setText));   // opened from the mic chip
            else if (!App.tv) getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
    }

    void showResults() {
        results.removeAllViews();
        firstCard = null;
        String[] words = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        if (query.trim().isEmpty()) {
            TextView tip = text("Try: anime, sports, 4k, spanish, korean, p-stream, live tv…", sp(14, 13), MUTED, false);
            tip.setPadding(dp(4), dp(16), 0, 0);
            results.addView(tip);
            return;
        }

        List<History.Entry> seen = new ArrayList<>();
        for (History.Entry e : History.list()) if (matches(e.title + " " + e.site + " " + e.episode, words)) seen.add(e);

        List<Sites.Site> found = new ArrayList<>();
        for (Sites.Site s : Sites.all()) {
            String hay = s.name + " " + s.tags + " " + s.group + " " + android.net.Uri.parse(s.url).getHost();
            if (matches(hay, words)) found.add(s);
        }
        final String first = words[0];
        found.sort((a, b) -> Integer.compare(rank(a, first), rank(b, first)));

        String q = query.trim();
        boolean pending = q.length() >= 2 && !titlesFor.equals(q);
        boolean haveTitles = titlesFor.equals(q) && !titles.isEmpty(), failed = titlesFor.equals(q) && titlesFailed;
        if (pending) {
            results.addView(heading("Movies & series"));
            results.addView(searching());
        } else if (haveTitles) {
            results.addView(heading("Movies & series"));
            LinearLayout row = row(results);
            for (Catalog.Title t : titles) row.addView(posterCard(t, posterW()));
        } else if (failed) {
            results.addView(heading("Movies & series"));
            results.addView(note("Can't reach the movie catalog. Check WARP in Settings, then search again."));
        }
        if (seen.isEmpty() && found.isEmpty() && !haveTitles && !pending && !failed) {
            TextView none = text("Nothing found for “" + q + "”", sp(16, 14), MUTED, false);
            none.setPadding(dp(4), dp(16), 0, 0);
            results.addView(none);
            return;
        }
        if (!seen.isEmpty()) {
            results.addView(heading("You watched"));
            LinearLayout row = row(results);
            for (History.Entry e : seen) row.addView(historyCard(e));
        }
        if (!found.isEmpty()) {
            boolean cut = found.size() > MAX_SITES;
            results.addView(heading("Sites  ·  " + found.size() + (cut ? " (showing " + MAX_SITES + ")" : "")));
            Grid grid = new Grid(results);
            for (Sites.Site s : cut ? found.subList(0, MAX_SITES) : found) grid.add(s);
        }
    }

    /** Asks the catalog for movies and series matching what is typed (after a short pause), then redraws. */
    void searchTitles() {
        final String q = query.trim();
        if (q.length() < 2 || q.equals(titlesFor)) return;
        String extra = "search=" + android.net.Uri.encode(q);
        Warp.whenSettled(() -> CompletableFuture.supplyAsync(() -> Catalog.list("movie", extra), lane)   // movies and series at the same time
            .thenCombine(CompletableFuture.supplyAsync(() -> Catalog.list("series", extra), lane), (movies, series) -> {
                List<Catalog.Title> all = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    if (movies != null && i < movies.size()) all.add(movies.get(i));
                    if (series != null && i < series.size()) all.add(series.get(i));
                }
                // Cinemeta also returns loose matches (a series named "...Ankara..." for "kantara"): titles with every typed word come first
                String[] words = q.toLowerCase(Locale.ROOT).split("\\s+");
                all.sort((a, b) -> Boolean.compare(!matches(a.name, words), !matches(b.name, words)));
                boolean failed = movies == null && series == null;
                runOnUiThread(() -> {
                    if (!q.equals(query.trim())) return;
                    titles = all;
                    titlesFor = q;
                    titlesFailed = failed;
                    showResults();
                });
                return null;
            }));
    }

    /** A spinner and a line while the catalog answers. */
    LinearLayout searching() {
        LinearLayout line = new LinearLayout(this);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(dp(4), dp(8), 0, dp(8));
        android.widget.ProgressBar spin = new android.widget.ProgressBar(this);
        spin.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ACCENT));
        line.addView(spin, new LinearLayout.LayoutParams(dp(28), dp(28)));
        TextView t = text("Searching movies and series\u2026", sp(15, 13), MUTED, false);
        t.setPadding(dp(12), 0, 0, 0);
        line.addView(t);
        return line;
    }

    /** Every typed word must appear somewhere in the text, accents ignored ("amelie" finds Amélie). */
    static boolean matches(String text, String[] words) {
        String t = plain(text);
        for (String w : words) if (!w.isEmpty() && !t.contains(plain(w))) return false;
        return true;
    }

    static String plain(String s) {
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Name starts with it, then name contains it, then FMHY's picks, then the rest. */
    static int rank(Sites.Site s, String word) {
        String n = s.name.toLowerCase(Locale.ROOT);
        return (n.startsWith(word) ? 0 : n.contains(word) ? 2 : 4) + (s.pick ? 0 : 1);
    }
}
