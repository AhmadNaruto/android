package me.champdev.weblock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * SubDL (subdl.com) as a subtitle source. Each user pastes their own free API key (2,000 searches and 50 keyed downloads a day).
 * Plain Java (no Android) so test/SubDlTest.java runs it; the app hands in its own (WARP aware) connection.
 */
final class SubDl {
    /** One GET, the answer's body. On an HTTP error return its body when there is one: SubDL's message for a bad key or quota is in it. */
    interface Http { InputStream open(String url) throws IOException; }

    static final class Result {
        String url;      // absolute .zip link; works without the key (see result())
        String lang;     // our 3-letter code, as Subs gets them from OpenSubtitles
        String release;
        int downloads;   // 0: the API sends no count today
        boolean hearingImpaired;
        boolean pack;    // the zip holds a whole season or several episodes
    }

    static final String API = "https://api.subdl.com/api/v1/subtitles";
    static final String DL = "https://dl.subdl.com";

    // SubDL's language list (subdl.com/api-files/language_list.json plus HY..SU added since) as "SUBDL ours".
    // Ours follow OpenSubtitles' codes: 639-2/B, but nld ron ell zho as there; pob and zht are theirs. MNI and the bilingual BG_EN, NL_EN, EN_DE, HU_EN fall through.
    private static final String TABLE = "AR ara,BR_PT pob,DA dan,NL nld,EN eng,FA per,FI fin,FR fre,ID ind,IT ita,NO nor,RO ron,ES spa,SV swe,VI vie,SQ alb,AZ aze,BE bel,BN ben,"
            + "ZH_BG zht,BS bos,BG bul,MY bur,CA cat,ZH zho,HR hrv,CS cze,EO epo,ET est,KA geo,DE ger,EL ell,KL kal,HE heb,HI hin,HU hun,IS ice,JA jpn,KO kor,KU kur,LV lav,LT lit,"
            + "MK mac,MS may,ML mal,PL pol,PT por,RU rus,SR srp,SI sin,SK slo,SL slv,TL tgl,TA tam,TE tel,TH tha,TR tur,UK ukr,UR urd,HY arm,KK kaz,KY kir,KM khm,KN kan,MN mon,"
            + "EU baq,GL glg,GA gle,JV jav,SU sun";
    // Other spellings of the same language that langParam accepts: 639-2/T (what Locale gives) and 639-2/B
    private static final String ALIASES = "fra fre,deu ger,ces cze,fas per,sqi alb,eus baq,isl ice,mkd mac,msa may,slk slo,kat geo,hye arm,mya bur,dut nld,rum ron,gre ell,chi zho";
    private static final Map<String, String> FROM = new HashMap<>(), TO = new HashMap<>(), ALIAS = new HashMap<>();

    static {
        for (String p : TABLE.split(",")) { String[] kv = p.split(" "); FROM.put(kv[0], kv[1]); TO.put(kv[1], kv[0]); }
        for (String p : ALIASES.split(",")) { String[] kv = p.split(" "); ALIAS.put(kv[0], kv[1]); }
    }

    /** SubDL's language code (AR, BR_PT) as our 3-letter one (ara, pob); unknown codes come back lower-cased. */
    static String langCode(String subdl) {
        String c = subdl == null ? "" : subdl.trim();
        if (c.isEmpty()) return "und";
        String o = FROM.get(c.toUpperCase(Locale.ROOT));
        return o != null ? o : c.toLowerCase(Locale.ROOT);
    }

    /** Our 3-letter code (also the /T and /B spellings) as SubDL's for the languages parameter; unknown ones upper-cased. */
    static String langParam(String iso3) {
        String c = iso3 == null ? "" : iso3.trim().toLowerCase(Locale.ROOT);
        if (ALIAS.containsKey(c)) c = ALIAS.get(c);
        String s = TO.get(c);
        return s != null ? s : c.toUpperCase(Locale.ROOT);
    }

    /**
     * Subtitles (.zip only) for an IMDb id, best first (single episodes before packs; most downloaded first when SubDL ever sends a count), at most 100.
     * season 0 = a movie. langs: comma separated 3-letter codes, "" = all. IOException carries SubDL's own message (bad key, quota), never the key.
     */
    static List<Result> search(Http http, String apiKey, String imdb, int season, int episode, String langs) throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) throw new IOException("SubDL needs an API key");
        String key = apiKey.trim();
        List<Result> out = new ArrayList<>();
        if (imdb == null || imdb.trim().isEmpty()) return out;
        Set<String> codes = new LinkedHashSet<>(), want = new HashSet<>();
        for (String l : langs == null ? new String[0] : langs.split(",")) {
            if (l.trim().isEmpty()) continue;
            codes.add(langParam(l));
            want.add(langCode(langParam(l)));
        }
        // The API caps a page at 30 whatever subs_per_page says
        StringBuilder q = new StringBuilder(API).append("?api_key=").append(enc(key)).append("&imdb_id=").append(enc(imdb.trim())).append("&subs_per_page=30");
        q.append(season > 0 ? "&type=tv&season_number=" + season + "&episode_number=" + episode : "&type=movie");
        if (!codes.isEmpty()) q.append("&languages=").append(join(codes));
        for (int page = 1; page <= 4 && out.size() < 100; page++) {
            JSONObject o = get(http, q + (page > 1 ? "&page=" + page : ""), key);
            if (o == null) break;
            int before = out.size();
            JSONArray a = o.optJSONArray("subtitles");
            for (int i = 0; a != null && i < a.length(); i++) {
                Result r = result(a.optJSONObject(i));
                // An unknown language code is ignored by SubDL (it answers with every language): keep only what was asked for
                if (r != null && (want.isEmpty() || want.contains(r.lang))) out.add(r);
            }
            if (out.size() == before || page >= o.optInt("totalPages", 1)) break;
        }
        Collections.sort(out, (a, b) -> a.downloads != b.downloads ? b.downloads - a.downloads : Boolean.compare(a.pack, b.pack));
        if (out.size() > 100) out.subList(100, out.size()).clear();
        return out;
    }

    /** The parsed answer; null when SubDL knows no such title. */
    private static JSONObject get(Http http, String url, String key) throws IOException {
        String body;
        try (InputStream in = http.open(url)) {
            java.util.Scanner s = new java.util.Scanner(in, "UTF-8").useDelimiter("\\A");
            body = s.hasNext() ? s.next() : "";
        } catch (IOException e) {
            // The URL, and so the key, is in many connection errors
            throw new IOException(hide(e.getMessage() == null ? "SubDL request failed" : e.getMessage(), key));
        }
        JSONObject o;
        try {
            o = new JSONObject(body);
        } catch (JSONException e) {
            throw new IOException("SubDL answered with something that is not JSON");
        }
        if (o.optBoolean("status", true)) return o;
        String err = str(o, "error"), msg = str(o, "message");
        if (err.contains("can't find")) return null;
        throw new IOException(hide(!msg.isEmpty() ? msg : !err.isEmpty() ? err : "SubDL refused the request", key));
    }

    private static Result result(JSONObject s) {
        String u = s == null ? "" : str(s, "url");
        if (u.isEmpty()) return null;
        // The answer appends "?api_key=KEY" to every link, which makes the download count against the 50 a day;
        // the file comes just as well without it, so the key is cut off here and never leaves search()
        if (u.indexOf('?') >= 0) u = u.substring(0, u.indexOf('?'));
        // Old uploads are bare .srt/.txt or .rar (no way to unpack those on the phone)
        if (!u.toLowerCase(Locale.ROOT).endsWith(".zip")) return null;
        Result r = new Result();
        r.url = u.startsWith("http") ? u : DL + (u.startsWith("/") ? "" : "/") + u;
        r.lang = langCode(str(s, "language"));
        r.release = str(s, "release_name").isEmpty() ? str(s, "name") : str(s, "release_name");
        r.downloads = s.optInt("downloads", 0);
        r.hearingImpaired = s.optBoolean("hi");
        r.pack = s.optBoolean("full_season") || s.optInt("episode_end") > s.optInt("episode_from");
        return r;
    }

    // "" for a missing or null value (Android's org.json would make "null" of it)
    private static String str(JSONObject o, String k) {
        return o.isNull(k) ? "" : o.optString(k, "");
    }

    private static String join(Set<String> s) throws IOException {
        StringBuilder b = new StringBuilder();
        for (String x : s) b.append(b.length() > 0 ? "," : "").append(enc(x));
        return b.toString();
    }

    private static String hide(String s, String key) throws IOException {
        return s.replace(key, "***").replace(enc(key), "***");
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8");
    }
}
