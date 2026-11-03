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
                s.release = o.optString("subtitleFileName", "").isEmpty() ? o.optString("movieReleaseName", "Subtitle " + (i + 1)) : o.optString("subtitleFileName");
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

    /** Languages with their files, your own language and English first, then the biggest. Keys are ISO 639-2 codes. */
    static Map<String, List<Sub>> byLanguage(List<Sub> all) {
        Map<String, List<Sub>> m = new LinkedHashMap<>();
        for (Sub s : all) m.computeIfAbsent(s.lang, k -> new ArrayList<>()).add(s);
        List<Map.Entry<String, List<Sub>>> es = new ArrayList<>(m.entrySet());
        String mine = Locale.getDefault().getISO3Language();
        es.sort((a, b) -> rank(b, mine) - rank(a, mine));
        Map<String, List<Sub>> out = new LinkedHashMap<>();
        for (Map.Entry<String, List<Sub>> e : es) out.put(e.getKey(), e.getValue());
        return out;
    }

    private static int rank(Map.Entry<String, List<Sub>> e, String mine) {
        return (e.getKey().equals(mine) ? 2000 : e.getKey().equals("eng") ? 1000 : 0) + e.getValue().size();
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
        String n = new Locale(iso3).getDisplayLanguage(Locale.ENGLISH);
        return n.isEmpty() || n.equals(iso3) ? iso3.toUpperCase(Locale.ROOT) : n;
    }
}
