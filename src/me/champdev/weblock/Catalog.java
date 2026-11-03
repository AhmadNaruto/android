package me.champdev.weblock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Movies and series from Cinemeta (Stremio's public catalog, no key): popular lists, search, details with episodes. */
final class Catalog {
    static final String BASE = "https://v3-cinemeta.strem.io";
    static final long TTL = 6L * 3600 * 1000;
    static final String[] GENRES = {"Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary", "Drama", "Family",
        "Fantasy", "History", "Horror", "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western"};

    static final class Episode {
        int season, number;
        String title = "", overview = "", thumb = "";
    }

    static final class Title {
        String tmdb = "";   // TheMovieDB id (Cinemeta meta only): many sites key their pages by it
        String id = "", type = "movie", name = "", year = "", rating = "", poster = "", background = "", overview = "", runtime = "", genres = "";
        final List<Episode> episodes = new ArrayList<>();

        boolean series() { return type.equals("series"); }
    }

    /** One catalog page. type = movie|series; extra = "" | "genre=Comedy" | "skip=50" | "search=text". Null when offline with nothing cached. */
    static List<Title> list(String type, String extra) {
        String text = get("/catalog/" + type + "/top" + (extra.isEmpty() ? "" : "/" + extra) + ".json", !extra.startsWith("search="));
        return text == null ? null : parseList(text, type);
    }

    /** The list if a fresh copy is already on disk (no network), else null: lets a row draw without waiting. */
    static List<Title> cached(String type, String extra) {
        File f = file("/catalog/" + type + "/top" + (extra.isEmpty() ? "" : "/" + extra) + ".json");
        if (!f.exists() || System.currentTimeMillis() - f.lastModified() > TTL) return null;
        try {
            return parseList(Sites.readAll(new java.io.FileInputStream(f)), type);
        } catch (IOException e) {
            return null;
        }
    }

    /** Fills in the overview, runtime, backdrop and episodes of a title from its detail page. */
    static void fill(Title t) {
        String text = get("/meta/" + t.type + "/" + t.id + ".json", true);
        if (text == null) return;
        try {
            Title full = parse(new JSONObject(text).getJSONObject("meta"), t.type);
            t.overview = full.overview;
            t.runtime = full.runtime;
            t.background = full.background;
            t.genres = full.genres;
            if (!full.tmdb.isEmpty()) t.tmdb = full.tmdb;
            if (!full.year.isEmpty()) t.year = full.year;
            if (!full.rating.isEmpty()) t.rating = full.rating;
            t.episodes.clear();
            t.episodes.addAll(full.episodes);
        } catch (JSONException ignored) { }
    }

    static List<Title> parseList(String json, String type) {
        List<Title> out = new ArrayList<>();
        try {
            JSONArray metas = new JSONObject(json).getJSONArray("metas");
            for (int i = 0; i < metas.length(); i++) {
                Title t = parse(metas.getJSONObject(i), type);
                if (!t.id.isEmpty() && !t.name.isEmpty()) out.add(t);
            }
        } catch (JSONException ignored) { }
        return out;
    }

    static Title parse(JSONObject o, String type) throws JSONException {
        Title t = new Title();
        t.type = type;
        t.id = o.optString("id");
        t.tmdb = o.optInt("moviedb_id") > 0 ? String.valueOf(o.optInt("moviedb_id")) : "";
        t.name = o.optString("name");
        t.year = o.optString("releaseInfo", o.optString("year"));
        t.rating = o.optString("imdbRating");
        // "small" posters are blurry on a TV card
        t.poster = o.optString("poster").replace("/poster/small/", "/poster/medium/");
        t.background = o.optString("background");
        t.overview = o.optString("description");
        t.runtime = o.optString("runtime");
        JSONArray g = o.optJSONArray("genres");
        if (g == null) g = o.optJSONArray("genre");
        List<String> names = new ArrayList<>();
        for (int i = 0; g != null && i < g.length(); i++) names.add(g.getString(i));
        t.genres = String.join(" · ", names);
        JSONArray videos = o.optJSONArray("videos");
        for (int i = 0; videos != null && i < videos.length(); i++) {
            JSONObject v = videos.getJSONObject(i);
            Episode e = new Episode();
            e.season = v.optInt("season");
            e.number = v.optInt("episode", v.optInt("number"));
            // Season 0 is "Specials" and entries without a number are placeholders
            if (e.season < 1 || e.number < 1) continue;
            e.title = v.optString("name", v.optString("title"));
            e.overview = v.optString("overview", v.optString("description"));
            e.thumb = v.optString("thumbnail");
            t.episodes.add(e);
        }
        return t;
    }

    static int seasons(Title t) {
        int n = 0;
        for (Episode e : t.episodes) n = Math.max(n, e.season);
        return n;
    }

    // ---- disk cache: a stale copy is used when the network fails, so the home screen still has posters offline ----

    static File file(String path) {
        try {
            byte[] sha = MessageDigest.getInstance("SHA-1").digest(path.getBytes(StandardCharsets.UTF_8));
            StringBuilder name = new StringBuilder();
            for (byte x : sha) name.append(String.format("%02x", x));
            File dir = new File(App.app.getFilesDir(), "catalog");
            dir.mkdirs();
            return new File(dir, name.toString());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    static String get(String path, boolean keep) {
        File f = file(path);
        try {
            if (f.exists() && System.currentTimeMillis() - f.lastModified() < TTL) return Sites.readAll(new java.io.FileInputStream(f));
            String text = Sites.readAll(Warp.open(BASE + path).getInputStream());
            if (keep) try (FileOutputStream out = new FileOutputStream(f)) { out.write(text.getBytes(StandardCharsets.UTF_8)); }
            return text;
        } catch (IOException e) {
            try {
                return f.exists() ? Sites.readAll(new java.io.FileInputStream(f)) : null;
            } catch (IOException again) {
                return null;
            }
        }
    }

    // ---- My list ----

    static List<Title> mine() {
        List<Title> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(App.prefs.getString("mylist", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Title t = new Title();
                t.id = o.getString("id");
                t.type = o.optString("type", "movie");
                t.name = o.optString("name");
                t.year = o.optString("year");
                t.poster = o.optString("poster");
                out.add(t);
            }
        } catch (JSONException ignored) { }
        return out;
    }

    static boolean isMine(String id) {
        for (Title t : mine()) if (t.id.equals(id)) return true;
        return false;
    }

    /** Adds (newest first) or removes; true if the title is now in the list. */
    static boolean toggleMine(Title t) {
        try {
            JSONArray old = new JSONArray(App.prefs.getString("mylist", "[]")), now = new JSONArray();
            boolean had = false;
            now.put(new JSONObject().put("id", t.id).put("type", t.type).put("name", t.name).put("year", t.year).put("poster", t.poster));
            for (int i = 0; i < old.length(); i++) {
                if (old.getJSONObject(i).getString("id").equals(t.id)) had = true;
                else now.put(old.get(i));
            }
            if (had) {
                now = new JSONArray();
                for (int i = 0; i < old.length(); i++) if (!old.getJSONObject(i).getString("id").equals(t.id)) now.put(old.get(i));
            }
            App.prefs.edit().putString("mylist", now.toString()).apply();
            return !had;
        } catch (JSONException e) {
            return false;
        }
    }
}
