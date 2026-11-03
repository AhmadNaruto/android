package me.champdev.weblock;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** "Continue watching": the last title opened on each site, newest first. */
final class History {
    static final int MAX = 60;
    static final Pattern CONTENT = Pattern.compile("(?i)/(movies?|tv|series|shows?|watch|episodes?|anime|films?|drama|title|play|stream|live|match|event|channel)([/-]|$)");
    static final Pattern NOT_CONTENT = Pattern.compile("(?i)/(search|genres?|category|categories|browse|filter|trending|popular|top|list|login|account)([/-]|$)");
    static final Pattern EP_TEXT = Pattern.compile("(?i)\\b(?:S(\\d{1,2})\\s*E(\\d{1,4})|Season\\s*(\\d+)\\W*Episode\\s*(\\d+)|(?:Episode|Ep\\.?)\\s*(\\d+))\\b");
    static final Pattern[] EP_URL = {
        Pattern.compile("(?i)/(?:tv|series|show)/[^/?#]+/(\\d{1,2})/(\\d{1,4})(?:[/?#]|$)"),
        Pattern.compile("(?i)season[-/=](\\d+).*?episode[-/=](\\d+)"),
        Pattern.compile("(?i)[?&]s=(\\d+).*?[?&]e=(\\d+)"),
    };
    static final Pattern EP_ONLY_URL = Pattern.compile("(?i)(?:episode|ep)[-/=](\\d+)");
    // Site taglines like "Movies, TV Shows & Anime" (shown before a page sets its real title)
    static final Pattern GENERIC = Pattern.compile("(?i)^(watch|free|online|hd|movies?|films?|tv|shows?|series|anime|dramas?|cartoons?|stream(ing)?|and|for|in|[,&|·:\\s-])+$");
    static final Pattern JUNK_TAIL = Pattern.compile("(?i)\\s+(online|free|hd|streaming|full movie|for free|eng(lish)? sub(bed)?|subbed|dubbed)\\b.*$");

    static final class Entry {
        String site, siteUrl, title, url, image, episode;
        long time;
    }

    static boolean isContent(String url) {
        String path = String.valueOf(Uri.parse(url).getPath());
        return path.length() > 1 && CONTENT.matcher(path).find() && !NOT_CONTENT.matcher(path).find();
    }

    static List<Entry> list() {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(App.prefs.getString("history", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Entry e = new Entry();
                e.site = o.getString("site");
                e.siteUrl = o.getString("siteUrl");
                e.title = o.getString("title");
                e.url = o.getString("url");
                e.image = o.optString("image");
                e.episode = o.optString("episode");
                e.time = o.getLong("time");
                out.add(e);
            }
        } catch (JSONException ignored) { }
        return out;
    }

    static void save(List<Entry> list) {
        JSONArray a = new JSONArray();
        try {
            for (Entry e : list.subList(0, Math.min(MAX, list.size())))
                a.put(new JSONObject().put("site", e.site).put("siteUrl", e.siteUrl).put("title", e.title).put("url", e.url)
                    .put("image", e.image).put("episode", e.episode).put("time", e.time));
        } catch (JSONException ignored) { }
        App.prefs.edit().putString("history", a.toString()).apply();
    }

    /** Records a page if it looks like a movie/episode/stream page; same title on same site updates in place. */
    static void record(String site, String siteUrl, String url, String rawTitle, String image) {
        if (!isContent(url) || rawTitle == null) return;
        String title = cleanTitle(rawTitle, site);
        String episode = episode(url, rawTitle);
        title = EP_TEXT.matcher(title).replaceAll("").replaceAll("[\\s:,|–-]+$", "").trim();
        if (title.length() < 2 || title.equalsIgnoreCase(site) || GENERIC.matcher(title).matches()) return;

        List<Entry> list = list();
        Entry e = null;
        for (Entry x : list) if (x.site.equals(site) && x.title.equalsIgnoreCase(title)) e = x;
        if (e == null) {
            e = new Entry();
            e.site = site;
            e.title = title;
            e.image = "";
        } else {
            list.remove(e);
        }
        e.siteUrl = siteUrl;
        e.url = url;
        // TMDB "original" files are huge; w780 is plenty for a card
        if (image != null && isTmdb(image)) image = image.replaceFirst("/t/p/[^/]+/", "/t/p/w780/");
        // First good poster sticks; a TMDB poster may replace a non-TMDB one, never the other way round
        if (image != null && !image.isEmpty() && (e.image.isEmpty() || (isTmdb(image) && !isTmdb(e.image)))) e.image = image;
        if (!episode.isEmpty() || e.episode == null) e.episode = episode;
        e.time = System.currentTimeMillis();
        list.add(0, e);
        save(list);
    }

    static boolean isTmdb(String url) {
        return url.contains("image.tmdb.org") || url.contains("/t/p/");
    }

    static void remove(Entry e) {
        List<Entry> list = list();
        list.removeIf(x -> x.site.equals(e.site) && x.title.equals(e.title));
        save(list);
    }

    /** "Watch Breaking Bad Online Free | Flixer" -> "Breaking Bad" */
    static String cleanTitle(String t, String site) {
        String best = "";
        for (String part : t.split("\\s+[|–—•·-]\\s+")) {
            String p = part.trim();
            if (p.isEmpty() || p.toLowerCase().contains(site.toLowerCase())) continue;
            best = p;
            break;
        }
        best = best.replaceFirst("(?i)^(watch|stream)\\s+", "");
        return JUNK_TAIL.matcher(best).replaceFirst("").trim();
    }

    static String episode(String url, String title) {
        for (Pattern p : EP_URL) {
            Matcher m = p.matcher(url);
            if (m.find()) return "S" + Integer.parseInt(m.group(1)) + " E" + Integer.parseInt(m.group(2));
        }
        Matcher t = EP_TEXT.matcher(title);
        if (t.find()) {
            if (t.group(1) != null) return "S" + Integer.parseInt(t.group(1)) + " E" + Integer.parseInt(t.group(2));
            if (t.group(3) != null) return "S" + Integer.parseInt(t.group(3)) + " E" + Integer.parseInt(t.group(4));
            return "E" + Integer.parseInt(t.group(5));
        }
        Matcher u = EP_ONLY_URL.matcher(url);
        return u.find() ? "E" + Integer.parseInt(u.group(1)) : "";
    }
}
