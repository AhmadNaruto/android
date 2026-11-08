package me.champdev.weblock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemClock;
import android.text.format.Formatter;
import android.webkit.CookieManager;
import android.widget.Toast;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.ResolvingDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.NoOpCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadHelper;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadNotificationHelper;
import androidx.media3.exoplayer.offline.DownloadRequest;
import androidx.media3.exoplayer.offline.DownloadService;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages video downloads, offline cache, metadata, and quality selection.
 */
@UnstableApi
public final class Downloads {
    private static SimpleCache cache;
    private static StandaloneDatabaseProvider databaseProvider;
    private static DownloadManager downloadManager;
    private static DownloadNotificationHelper notificationHelper;
    private static final Map<String, Map<String, String>> headersMap = new ConcurrentHashMap<>();

    public static final class Entry {
        public String id = "";
        public String url = "";
        public String title = "";
        public String episode = "";
        public String image = "";
        public String mime = "";
        public boolean dash;
        public String headersJson = "";
        public List<String> subs = new ArrayList<>();
        public long createdAt;
        public long bytes;
        public float percent;
        public int state = Download.STATE_QUEUED;
    }

    public static synchronized SimpleCache getCache(Context context) {
        if (cache == null) {
            Context app = context.getApplicationContext();
            File dir = app.getExternalFilesDir(null);
            if (dir == null) dir = app.getFilesDir();
            File downloadDir = new File(dir, "downloads");
            StandaloneDatabaseProvider db = getDatabaseProvider(app);
            cache = new SimpleCache(downloadDir, new NoOpCacheEvictor(), db);
        }
        return cache;
    }

    public static synchronized StandaloneDatabaseProvider getDatabaseProvider(Context context) {
        if (databaseProvider == null) {
            databaseProvider = new StandaloneDatabaseProvider(context.getApplicationContext());
        }
        return databaseProvider;
    }

    public static synchronized DownloadNotificationHelper getNotificationHelper(Context context) {
        if (notificationHelper == null) {
            notificationHelper = new DownloadNotificationHelper(context.getApplicationContext(), VideoDownloadService.CHANNEL_ID);
        }
        return notificationHelper;
    }

    public static synchronized DownloadManager getDownloadManager(Context context) {
        if (downloadManager == null) {
            Context app = context.getApplicationContext();
            DataSource.Factory upstream = createUpstreamFactory(app);
            downloadManager = new DownloadManager(app, getDatabaseProvider(app), getCache(app), upstream, App.io);
            downloadManager.setMaxParallelDownloads(3);
            downloadManager.addListener(new DownloadManager.Listener() {
                @Override
                public void onDownloadChanged(DownloadManager dm, Download d, Exception e) {
                    updateFromDownload(app, d);
                }

                @Override
                public void onDownloadRemoved(DownloadManager dm, Download d) {
                    removeEntry(app, d.request.id);
                }
            });
            initHeadersFromSaved(app);
        }
        return downloadManager;
    }

    public static DataSource.Factory createUpstreamFactory(Context context) {
        String ua = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36";
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
            .setUserAgent(ua)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000);
        return new ResolvingDataSource.Factory(http, spec -> {
            String u = spec.uri.toString();
            Map<String, String> extra = new HashMap<>();
            String cookie = CookieManager.getInstance().getCookie(u);
            if (cookie != null && !cookie.isEmpty()) extra.put("Cookie", cookie);
            Map<String, String> h = findHeaders(u);
            if (h != null) extra.putAll(h);
            return extra.isEmpty() ? spec : spec.withAdditionalHeaders(extra);
        });
    }

    private static Map<String, String> findHeaders(String url) {
        if (url == null) return null;
        for (Map.Entry<String, Map<String, String>> en : headersMap.entrySet()) {
            if (url.startsWith(en.getKey()) || en.getKey().startsWith(url)) {
                return en.getValue();
            }
        }
        return null;
    }

    public static void rememberHeaders(String url, String headersJson) {
        if (url == null || headersJson == null || headersJson.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(headersJson);
            Map<String, String> map = new HashMap<>();
            for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
                String k = it.next();
                map.put(k, obj.getString(k));
            }
            headersMap.put(url, map);
        } catch (Exception ignored) { }
    }

    private static void initHeadersFromSaved(Context context) {
        for (Entry e : list()) {
            if (!e.headersJson.isEmpty()) {
                rememberHeaders(e.url, e.headersJson);
            }
        }
    }

    // ---- Metadata persistence ----

    public static synchronized List<Entry> list() {
        List<Entry> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(App.prefs.getString("downloads_meta", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Entry e = new Entry();
                e.id = o.optString("id");
                e.url = o.optString("url");
                e.title = o.optString("title");
                e.episode = o.optString("episode");
                e.image = o.optString("image");
                e.mime = o.optString("mime");
                e.dash = o.optBoolean("dash");
                e.headersJson = o.optString("headers");
                e.createdAt = o.optLong("time");
                e.bytes = o.optLong("bytes");
                e.percent = (float) o.optDouble("percent");
                e.state = o.optInt("state");
                JSONArray sa = o.optJSONArray("subs");
                if (sa != null) {
                    for (int j = 0; j < sa.length(); j++) e.subs.add(sa.getString(j));
                }
                out.add(e);
            }
        } catch (Exception ignored) { }
        return out;
    }

    private static synchronized void saveList(List<Entry> entries) {
        try {
            JSONArray arr = new JSONArray();
            for (Entry e : entries) {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("url", e.url);
                o.put("title", e.title);
                o.put("episode", e.episode);
                o.put("image", e.image);
                o.put("mime", e.mime);
                o.put("dash", e.dash);
                o.put("headers", e.headersJson);
                o.put("time", e.createdAt);
                o.put("bytes", e.bytes);
                o.put("percent", e.percent);
                o.put("state", e.state);
                JSONArray sa = new JSONArray();
                for (String s : e.subs) sa.put(s);
                o.put("subs", sa);
                arr.put(o);
            }
            App.prefs.edit().putString("downloads_meta", arr.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static synchronized Entry getEntry(String url) {
        if (url == null) return null;
        for (Entry e : list()) {
            if (url.equals(e.url) || url.equals(e.id)) return e;
        }
        return null;
    }

    public static synchronized boolean isDownloaded(String url) {
        Entry e = getEntry(url);
        return e != null && e.state == Download.STATE_COMPLETED;
    }

    public static synchronized boolean isDownloading(String url) {
        Entry e = getEntry(url);
        return e != null && (e.state == Download.STATE_DOWNLOADING || e.state == Download.STATE_QUEUED);
    }

    public static synchronized boolean isDownloading() {
        for (Entry e : list()) {
            if (e.state == Download.STATE_DOWNLOADING || e.state == Download.STATE_QUEUED) return true;
        }
        return false;
    }

    public static synchronized boolean hasDownloads() {
        return !list().isEmpty();
    }

    private static synchronized void updateFromDownload(Context context, Download d) {
        List<Entry> entries = list();
        Entry match = null;
        for (Entry e : entries) {
            if (d.request.id.equals(e.id) || d.request.id.equals(e.url)) {
                match = e;
                break;
            }
        }
        if (match != null) {
            match.state = d.state;
            match.bytes = d.getBytesDownloaded();
            match.percent = d.getPercentDownloaded();
            saveList(entries);

            if (d.state == Download.STATE_COMPLETED) {
                notifyCompleted(context, match);
            } else if (d.state == Download.STATE_FAILED) {
                notifyFailed(context, match);
            }
        }
    }

    private static synchronized void removeEntry(Context context, String id) {
        List<Entry> entries = list();
        for (int i = 0; i < entries.size(); i++) {
            if (id.equals(entries.get(i).id) || id.equals(entries.get(i).url)) {
                entries.remove(i);
                break;
            }
        }
        saveList(entries);
        headersMap.remove(id);
    }

    private static void notifyCompleted(Context context, Entry e) {
        try {
            Map<String, String> headers = parseHeaders(e.headersJson);
            Intent intent = PlayerActivity.open(context, e.url, e.dash, headers, e.subs, e.title);
            PendingIntent pi = PendingIntent.getActivity(context, e.id.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            Notification n = getNotificationHelper(context).buildDownloadCompletedNotification(context, R.drawable.ic_p_download, pi, e.title);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(e.id.hashCode(), n);
        } catch (Exception ignored) { }
    }

    private static void notifyFailed(Context context, Entry e) {
        try {
            Notification n = getNotificationHelper(context).buildDownloadFailedNotification(context, R.drawable.ic_p_download, null, e.title);
            ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(e.id.hashCode(), n);
        } catch (Exception ignored) { }
    }

    public static Map<String, String> parseHeaders(String json) {
        Map<String, String> map = new HashMap<>();
        if (json == null || json.isEmpty()) return map;
        try {
            JSONObject o = new JSONObject(json);
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String k = it.next();
                map.put(k, o.getString(k));
            }
        } catch (Exception ignored) { }
        return map;
    }

    // ---- User Actions ----

    public static void prompt(Activity activity, String url, String title, String headersJson, String mime, boolean dash, Catalog.Title series, List<String> subs, ExoPlayer player) {
        if (url == null || activity == null) return;
        if (isDownloaded(url)) {
            new AlertDialog.Builder(activity)
                .setTitle(title != null && !title.isEmpty() ? title : App.t(R.string.player_downloaded))
                .setItems(new String[]{
                    App.t(R.string.player_download_play_offline),
                    App.t(R.string.player_download_delete)
                }, (d, i) -> {
                    if (i == 0) {
                        if (player != null) {
                            player.seekTo(0);
                            player.play();
                        } else {
                            activity.startActivity(PlayerActivity.open(activity, url, dash, parseHeaders(headersJson), subs != null ? subs : Collections.emptyList(), title));
                        }
                    } else if (i == 1) {
                        delete(activity, url);
                    }
                }).show();
            return;
        }

        if (isDownloading(url)) {
            Entry e = getEntry(url);
            String label = e != null ? String.format(Locale.ROOT, "%s (%.0f%%)", App.t(R.string.player_downloading, ""), e.percent) : App.t(R.string.player_download);
            new AlertDialog.Builder(activity)
                .setTitle(label)
                .setItems(new String[]{
                    App.t(R.string.player_download_cancel)
                }, (d, i) -> delete(activity, url)).show();
            return;
        }

        // Available video qualities
        if (player != null) {
            Tracks tracks = player.getCurrentTracks();
            List<Integer> heights = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            labels.add(App.t(R.string.download_best_quality));
            heights.add(0);

            for (Tracks.Group g : tracks.getGroups()) {
                if (g.getType() != C.TRACK_TYPE_VIDEO) continue;
                for (int t = 0; t < g.length; t++) {
                    if (!g.isTrackSupported(t)) continue;
                    Format f = g.getTrackFormat(t);
                    if (f.height > 0 && !heights.contains(f.height)) {
                        heights.add(f.height);
                        labels.add(f.height + "p" + (f.frameRate > 30 ? Math.round(f.frameRate) : "") + (f.bitrate > 0 ? " (" + Formatter.formatFileSize(activity, f.bitrate / 8) + "/s)" : ""));
                    }
                }
            }

            if (labels.size() > 2) {
                new AlertDialog.Builder(activity)
                    .setTitle(App.t(R.string.player_download))
                    .setItems(labels.toArray(new String[0]), (d, i) -> {
                        start(activity, url, title, headersJson, mime, dash, series, subs, heights.get(i));
                    }).show();
                return;
            }
        }

        start(activity, url, title, headersJson, mime, dash, series, subs, 0);
    }

    public static void start(Context context, String url, String title, String headersJson, String mime, boolean dash, Catalog.Title series, List<String> subs, int maxHeight) {
        rememberHeaders(url, headersJson);

        Entry e = new Entry();
        e.id = url;
        e.url = url;
        e.title = title != null && !title.isEmpty() ? title : url;
        if (series != null) {
            e.image = series.poster;
        }
        e.mime = mime != null ? mime : "";
        e.dash = dash;
        e.headersJson = headersJson != null ? headersJson : "";
        if (subs != null) e.subs.addAll(subs);
        e.createdAt = System.currentTimeMillis();
        e.state = Download.STATE_QUEUED;

        List<Entry> all = list();
        all.add(0, e);
        saveList(all);

        // Wake up WARP if enabled so download traffic routes seamlessly
        if (App.prefs.getBoolean("warp", true) && Warp.state == Warp.State.OFF) {
            Warp.start();
        }

        MediaItem.Builder ib = new MediaItem.Builder().setUri(url);
        if (!e.mime.isEmpty()) ib.setMimeType(e.mime);
        MediaItem mediaItem = ib.build();

        App.io.execute(() -> {
            try {
                DownloadHelper helper = DownloadHelper.forMediaItem(context, mediaItem, createUpstreamFactory(context));
                helper.prepare(new DownloadHelper.Callback() {
                    @Override
                    public void onPrepared(DownloadHelper h) {
                        if (maxHeight > 0) {
                            TrackSelectionParameters params = TrackSelectionParameters.DEFAULT_WITHOUT_CONTEXT.buildUpon()
                                .setMaxVideoSize(Integer.MAX_VALUE, maxHeight)
                                .build();
                            h.replaceTrackSelections(0, params);
                        }
                        DownloadRequest req = h.getDownloadRequest(url, (byte[]) null);
                        DownloadService.sendAddDownload(context, VideoDownloadService.class, req, false);
                        h.release();
                    }

                    @Override
                    public void onPrepareError(DownloadHelper h, IOException err) {
                        DownloadRequest.Builder b = new DownloadRequest.Builder(url, Uri.parse(url));
                        if (e.mime != null && !e.mime.isEmpty()) b.setMimeType(e.mime);
                        DownloadService.sendAddDownload(context, VideoDownloadService.class, b.build(), false);
                        h.release();
                    }
                });
            } catch (Exception ex) {
                DownloadRequest.Builder b = new DownloadRequest.Builder(url, Uri.parse(url));
                if (e.mime != null && !e.mime.isEmpty()) b.setMimeType(e.mime);
                DownloadService.sendAddDownload(context, VideoDownloadService.class, b.build(), false);
            }
        });

        Toast.makeText(context, App.t(R.string.download_started, e.title), Toast.LENGTH_SHORT).show();
    }

    public static void delete(Context context, String url) {
        if (url == null) return;
        DownloadService.sendRemoveDownload(context, VideoDownloadService.class, url, false);
        removeEntry(context, url);
        Toast.makeText(context, App.t(R.string.download_deleted), Toast.LENGTH_SHORT).show();
    }
}
