// Checks SubDl.java: parsing with canned answers (always), then the real API when SUBDL_KEY holds a free key (subdl.com/panel/api).
// SUBDL_NODL=1 skips the one real download, which counts against the key's 50 a day. From the project folder:
// JSON=~/.gradle/caches/modules-2/files-2.1/org.json/json/20240303/ebb88e8fb5122b7506d5cf1d69f1ccdb790d22a/json-20240303.jar
// javac --release 8 -cp $JSON -d /tmp/subdl src/me/champdev/weblock/SubDl.java test/SubDlTest.java && SUBDL_KEY=<your key> java -cp /tmp/subdl:$JSON me.champdev.weblock.SubDlTest
package me.champdev.weblock;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SubDlTest {
    static int fails;
    static final String KEY = "SECRETKEY123";

    static void check(String name, Object got, Object want) {
        if (got == null ? want != null : !got.equals(want)) { fails++; System.out.println("FAIL " + name + ": got " + got + " want " + want); }
    }

    /** Answers every request with `body`, noting the URLs asked. */
    static SubDl.Http canned(final String body, final List<String> asked) {
        return url -> { asked.add(url); return new ByteArrayInputStream(body.getBytes("UTF-8")); };
    }

    static String sub(String release, String language, String url, boolean hi, String episodeFields) {
        return "{\"release_name\":\"" + release + "\",\"name\":\"x.zip\",\"lang\":\"whatever\",\"author\":\"a\",\"url\":" + url + ",\"subtitlePage\":\"/s/info/x\",\"season\":1,\"language\":\"" + language
                + "\",\"framerate\":0,\"fps\":null,\"hi\":" + hi + "," + episodeFields + "}";
    }

    static final String MOVIE = "{\"status\":true,\"results\":[{\"sd_id\":21581,\"name\":\"The Matrix\",\"type\":\"movie\",\"imdb_id\":\"tt0133093\"}],\"subtitles\":["
            + sub("The.Matrix.1080p", "AR", "\"/subtitle/3443533-8366101.zip?api_key=" + KEY + "\"", false, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":false") + ","
            + sub("no url", "AR", "null", false, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":false") + ","
            + sub("Matrix.BR", "BR_PT", "\"/subtitle/1-2.zip?api_key=" + KEY + "\"", true, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":false") + ","
            + sub("Matrix.XX", "BG_EN", "\"/subtitle/3-4.zip\"", false, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":false") + ","
            + sub("old rar", "AR", "\"/subtitle/5-6.rar?api_key=" + KEY + "\"", false, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":false") + ","
            + sub("old srt", "AR", "\"/subtitle/7-8.srt\"", false, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":false")
            + "],\"totalPages\":1,\"currentPage\":1}";

    static final String EPISODE = "{\"status\":true,\"results\":[{\"name\":\"Breaking Bad\",\"type\":\"tv\"}],\"subtitles\":["
            + sub("Breaking.Bad.S01.Complete", "EN", "\"/subtitle/10-10.zip?api_key=" + KEY + "\"", false, "\"episode\":null,\"episode_from\":null,\"episode_end\":0,\"full_season\":true") + ","
            + sub("Breaking.Bad.S01E01-03", "EN", "\"/subtitle/11-11.zip?api_key=" + KEY + "\"", false, "\"episode\":null,\"episode_from\":1,\"episode_end\":3,\"full_season\":false") + ","
            + sub("breaking.bad.s01e01.720p", "EN", "\"/subtitle/12-12.zip?api_key=" + KEY + "\"", false, "\"episode\":1,\"episode_from\":1,\"episode_end\":1,\"full_season\":false")
            + "],\"totalPages\":1,\"currentPage\":1}";

    static String page(int n, int total) {
        JSONArray a = new JSONArray();
        for (int i = 0; i < 30; i++) {
            a.put(new JSONObject().put("release_name", "r" + n + "_" + i).put("language", "EN").put("url", "/subtitle/" + n + "-" + i + ".zip?api_key=" + KEY).put("hi", false));
        }
        return new JSONObject().put("status", true).put("subtitles", a).put("totalPages", total).put("currentPage", n).toString();
    }

    static String message(SubDl.Http http, String key) {
        try {
            SubDl.search(http, key, "tt0133093", 0, 0, "");
            return "no exception";
        } catch (IOException e) {
            return e.getMessage();
        }
    }

    public static void main(String[] a) throws Exception {
        // The movie request and what comes back
        List<String> asked = new ArrayList<>();
        List<SubDl.Result> r = SubDl.search(canned(MOVIE, asked), KEY, "tt0133093", 0, 0, "ara");
        check("movie request", asked, Arrays.asList("https://api.subdl.com/api/v1/subtitles?api_key=" + KEY + "&imdb_id=tt0133093&subs_per_page=30&type=movie&languages=AR"));
        check("entries without url or zip skipped, BG_EN filtered", r.size(), 1);
        check("key cut off the link", r.get(0).url, "https://dl.subdl.com/subtitle/3443533-8366101.zip");
        check("lang", r.get(0).lang, "ara");
        check("release", r.get(0).release, "The.Matrix.1080p");
        check("not hearing impaired", r.get(0).hearingImpaired, false);
        check("movie is no pack", r.get(0).pack, false);

        // No language filter: everything with a link, in the order SubDL gave, Brazilian Portuguese and the unmapped bilingual one included
        asked.clear();
        r = SubDl.search(canned(MOVIE, asked), KEY, "tt0133093", 0, 0, "");
        check("no filter: no languages parameter", asked.get(0).contains("languages="), false);
        check("no filter: 3 results", r.size(), 3);
        check("pob", r.get(1).lang, "pob");
        check("hi flag", r.get(1).hearingImpaired, true);
        check("unmapped bilingual lower-cased", r.get(2).lang, "bg_en");
        check("link that had no key stays", r.get(2).url, "https://dl.subdl.com/subtitle/3-4.zip");
        for (SubDl.Result x : r) check("no api_key in " + x.url, x.url.contains("api_key") || x.url.contains(KEY), false);

        // Several languages, our /T and /B spellings, a language SubDL does not have
        asked.clear();
        SubDl.search(canned(MOVIE, asked), KEY, "tt0133093", 0, 0, " ara, pob,fra,fre,zhe ,");
        check("language list", asked.get(0).substring(asked.get(0).indexOf("&languages=")), "&languages=AR,BR_PT,FR,ZHE");
        asked.clear();
        check("unknown language: SubDL answers with all, none kept", SubDl.search(canned(MOVIE, asked), KEY, "tt0133093", 0, 0, "zhe").size(), 0);
        check("unknown language: stops after one page", asked.size(), 1);

        // The episode request; single episodes before packs
        asked.clear();
        r = SubDl.search(canned(EPISODE, asked), KEY, "tt0903747", 1, 1, "eng");
        check("episode request", asked, Arrays.asList("https://api.subdl.com/api/v1/subtitles?api_key=" + KEY + "&imdb_id=tt0903747&subs_per_page=30&type=tv&season_number=1&episode_number=1&languages=EN"));
        check("3 results", r.size(), 3);
        check("single episode first", r.get(0).release, "breaking.bad.s01e01.720p");
        check("single is no pack", r.get(0).pack, false);
        check("full season is a pack", r.get(1).pack, true);
        check("episode range is a pack", r.get(2).pack, true);
        check("pack order kept", r.get(1).release, "Breaking.Bad.S01.Complete");

        // Most downloaded first, if SubDL ever sends a count
        JSONObject o = new JSONObject(page(1, 1));
        o.getJSONArray("subtitles").getJSONObject(3).put("downloads", 500);
        o.getJSONArray("subtitles").getJSONObject(7).put("downloads", 900);
        r = SubDl.search(canned(o.toString(), new ArrayList<String>()), KEY, "tt1", 0, 0, "");
        check("downloads: most first", r.get(0).downloads, 900);
        check("downloads: then", r.get(1).downloads, 500);
        check("downloads: the rest keep their order", r.get(2).release, "r1_0");

        // Paging: 30 a page, never more than 100 in all
        final List<String> reqs = new ArrayList<>();
        r = SubDl.search(url -> {
            reqs.add(url);
            int i = url.indexOf("&page=");
            return new ByteArrayInputStream(page(i < 0 ? 1 : Integer.parseInt(url.substring(i + 6)), 41).getBytes("UTF-8"));
        }, KEY, "tt0133093", 0, 0, "");
        check("100 kept", r.size(), 100);
        check("4 requests", reqs.size(), 4);
        check("first has no page", reqs.get(0).contains("&page="), false);
        check("then page 2", reqs.get(1).endsWith("&page=2"), true);
        check("then page 4", reqs.get(3).endsWith("&page=4"), true);
        reqs.clear();
        r = SubDl.search(url -> { reqs.add(url); return new ByteArrayInputStream(page(1, 1).getBytes("UTF-8")); }, KEY, "tt0133093", 0, 0, "");
        check("last page: 30", r.size(), 30);
        check("last page: 1 request", reqs.size(), 1);

        // Errors: SubDL's message, never the key
        String bad = "{\"status\":false,\"statusCode\":403,\"error\":\"not_authorized\",\"message\":\"Not Authorized\"}";
        check("bad key answer", message(canned(bad, new ArrayList<String>()), KEY), "Not Authorized");
        check("quota answer", message(canned("{\"status\":false,\"statusCode\":429,\"error\":\"daily_limit\"}", new ArrayList<String>()), KEY), "daily_limit");
        check("not JSON", message(canned("<html>Just a moment</html>", new ArrayList<String>()), KEY), "SubDL answered with something that is not JSON");
        check("empty body", message(canned("", new ArrayList<String>()), KEY), "SubDL answered with something that is not JSON");
        String m = message(url -> { throw new IOException("Server returned HTTP response code: 403 for URL: " + url); }, KEY);
        check("connection error keeps the news", m.startsWith("Server returned HTTP response code: 403 for URL: https://api.subdl.com/"), true);
        check("connection error hides the key", m.contains(KEY), false);
        check("key in a message is hidden", message(canned("{\"status\":false,\"message\":\"bad " + KEY + "\"}", new ArrayList<String>()), KEY).contains(KEY), false);
        String enc = "a b&c";
        m = message(url -> { throw new IOException(url); }, enc);
        check("encoded key hidden too", m.contains("a+b%26c") || m.contains(enc), false);
        int n = reqs.size();
        check("blank key", message(url -> { reqs.add(url); return null; }, "  "), "SubDL needs an API key");
        check("blank key: no request", reqs.size(), n);
        check("title SubDL does not know", SubDl.search(canned("{\"status\":false,\"error\":\"can't find movie or tv\"}", new ArrayList<String>()), KEY, "tt99999999", 0, 0, "").size(), 0);
        check("no imdb id: no request", SubDl.search(url -> { reqs.add(url); return null; }, KEY, "", 0, 0, "").size() + reqs.size() - n, 0);

        // Language codes, both ways
        String[][] pairs = {{"AR", "ara"}, {"EN", "eng"}, {"BR_PT", "pob"}, {"PT", "por"}, {"ZH", "zho"}, {"ZH_BG", "zht"}, {"DE", "ger"}, {"FR", "fre"}, {"NL", "nld"}, {"FA", "per"}, {"ES", "spa"}, {"RO", "ron"}, {"EL", "ell"}};
        for (String[] p : pairs) {
            check("langCode " + p[0], SubDl.langCode(p[0]), p[1]);
            check("langParam " + p[1], SubDl.langParam(p[1]), p[0]);
        }
        check("langCode is case-blind", SubDl.langCode("br_pt"), "pob");
        check("langParam fra (Locale's)", SubDl.langParam("fra"), "FR");
        check("langParam deu", SubDl.langParam("deu"), "DE");
        check("langParam dut", SubDl.langParam("dut"), "NL");
        check("langParam chi", SubDl.langParam("chi"), "ZH");
        check("langParam FRE", SubDl.langParam(" FRE "), "FR");
        check("unknown code lower-cased", SubDl.langCode("XX"), "xx");
        check("unknown code back upper-cased", SubDl.langParam("xx"), "XX");
        check("no code", SubDl.langCode(""), "und");
        check("null code", SubDl.langCode(null), "und");
        check("langParam of our odd OpenSubtitles one", SubDl.langParam("zhe"), "ZHE");
        // Every code in SubDL's language_list.json comes back as itself, and those with a 3-letter code get one
        String list = "AR BR_PT DA NL EN FA FI FR ID IT NO RO ES SV VI SQ AZ BE BN ZH_BG BS BG BG_EN MY CA ZH HR CS NL_EN EN_DE EO ET KA DE EL KL HE HI HU HU_EN IS JA KO KU LV LT MK MS ML MNI PL PT RU SR SI SK SL TL TA TE TH TR UK UR";
        HashSet<String> seen = new HashSet<>();
        List<String> bilingual = Arrays.asList("BG_EN", "NL_EN", "EN_DE", "HU_EN");
        for (String c : list.split(" ")) {
            String ours = SubDl.langCode(c);
            check("round trip " + c, SubDl.langParam(ours), c);
            check("3 letters, or lower-cased when SubDL's own mix " + c, bilingual.contains(c) ? ours : ours.length() + "", bilingual.contains(c) ? c.toLowerCase() : "3");
            check("distinct " + c, seen.add(ours), true);
        }
        check("all 64 codes tried", seen.size(), 64);

        if (System.getenv("SUBDL_KEY") == null) System.out.println("SKIP live checks (SUBDL_KEY not set)");
        else live(System.getenv("SUBDL_KEY").trim());

        if (fails > 0) { System.out.println(fails + " FAILED"); System.exit(1); }
        System.out.println("subdl tests passed");
    }

    static int requests;

    static void live(String key) throws Exception {
        SubDl.Http http = url -> {
            requests++;
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(30000);
            c.setRequestProperty("User-Agent", "FMHYPlay-test");
            return c.getResponseCode() >= 400 && c.getErrorStream() != null ? c.getErrorStream() : c.getInputStream();
        };
        List<SubDl.Result> r = SubDl.search(http, key, "tt0133093", 0, 0, "ara");
        System.out.println("live: The Matrix ara: " + r.size() + " results, first: " + (r.isEmpty() ? "-" : r.get(0).release));
        check("live movie has results", r.isEmpty(), false);
        for (SubDl.Result x : r) {
            if (!x.lang.equals("ara")) check("live lang", x.lang, "ara");
            if (!x.url.startsWith("https://dl.subdl.com/subtitle/") || !x.url.endsWith(".zip") || x.url.contains("api_key") || x.url.contains(key)) check("live link form", x.url, "https://dl.subdl.com/subtitle/..zip");
        }

        List<SubDl.Result> e = SubDl.search(http, key, "tt0903747", 1, 1, "eng");
        int packs = 0;
        for (SubDl.Result x : e) if (x.pack) packs++;
        System.out.println("live: Breaking Bad S01E01 eng: " + e.size() + " results, " + packs + " packs, first: " + (e.isEmpty() ? "-" : e.get(0).release));
        check("live episode has results", e.isEmpty(), false);
        for (SubDl.Result x : e) if (!x.lang.equals("eng")) check("live episode lang", x.lang, "eng");

        String msg = message(http, "not-a-key");
        System.out.println("live: bad key says: " + msg);
        check("live bad key refused", msg.equals("no exception") || msg.isEmpty() || msg.contains("not-a-key"), false);

        if (System.getenv("SUBDL_NODL") != null || r.isEmpty()) System.out.println("SKIP live download");
        else {
            // The link as search() hands it out (no key) must be a zip
            try (InputStream in = http.open(r.get(0).url)) {
                byte[] head = new byte[4];
                int got = 0, k;
                while (got < 4 && (k = in.read(head, got, 4 - got)) > 0) got += k;
                check("live download starts with PK", new String(head, 0, Math.min(got, 2), "ISO-8859-1"), "PK");
                System.out.println("live: download of the first result without the key: " + (got >= 2 ? (char) head[0] + "" + (char) head[1] : "nothing"));
            }
        }
        System.out.println("live: " + requests + " requests");
        check("under 15 live requests", requests < 15, true);
    }
}
