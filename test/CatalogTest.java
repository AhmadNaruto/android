// SDK=~/Library/Android/sdk; org.json from maven (android.jar only has stubs):
// javac -cp $SDK/platforms/android-34/android.jar:libs/androidx-webkit-1.12.1.jar:json.jar -sourcepath src -d /tmp/cat src/me/champdev/weblock/Catalog.java test/CatalogTest.java && java -cp /tmp/cat:json.jar me.champdev.weblock.CatalogTest
package me.champdev.weblock;

import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class CatalogTest {
    static int fails;
    static void check(String name, boolean ok) { if (!ok) { fails++; System.out.println("FAIL " + name); } }

    public static void main(String[] a) throws Exception {
        List<Catalog.Title> list = Catalog.parseList(new String(Files.readAllBytes(Paths.get("test/fixtures/catalog.json"))), "movie");
        check("catalog has 3 titles", list.size() == 3);
        Catalog.Title t = list.get(0);
        check("title has id and name", t.id.startsWith("tt") && !t.name.isEmpty());
        check("poster upgraded from small to medium", !t.poster.contains("/poster/small/") && t.poster.startsWith("http"));
        check("movie type kept", t.type.equals("movie") && !t.series());

        JSONObject meta = new JSONObject(new String(Files.readAllBytes(Paths.get("test/fixtures/series-meta.json")))).getJSONObject("meta");
        Catalog.Title s = Catalog.parse(meta, "series");
        check("series name", s.name.equals("Breaking Bad"));
        check("genres joined", s.genres.contains("Crime") && s.genres.contains(" · "));
        check("specials (season 0) dropped", s.episodes.stream().noneMatch(e -> e.season < 1));
        check("one season counted", Catalog.seasons(s) == 1);
        check("episode fields", !s.episodes.isEmpty() && s.episodes.get(0).number >= 1 && !s.episodes.get(0).title.isEmpty());
        check("overview present", !s.overview.isEmpty());
        check("garbage gives empty list", Catalog.parseList("<html>nope</html>", "movie").isEmpty());
        System.out.println(fails == 0 ? "catalog tests passed" : fails + " failed");
        System.exit(fails == 0 ? 0 : 1);
    }
}
