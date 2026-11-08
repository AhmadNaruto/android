package me.champdev.weblock;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;

import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.offline.Download;
import androidx.media3.exoplayer.offline.DownloadManager;
import androidx.media3.exoplayer.offline.DownloadNotificationHelper;
import androidx.media3.exoplayer.offline.DownloadService;
import androidx.media3.exoplayer.scheduler.Scheduler;

import java.util.List;

/**
 * Background foreground service managing Media3 video downloads and notifications.
 */
@UnstableApi
public class VideoDownloadService extends DownloadService {
    public static final String CHANNEL_ID = "video_downloads";
    public static final int NOTIFICATION_ID = 2001;

    public VideoDownloadService() {
        super(
            NOTIFICATION_ID,
            DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
            CHANNEL_ID,
            R.string.download_channel_name,
            R.string.download_channel_desc
        );
    }

    @Override
    protected DownloadManager getDownloadManager() {
        return Downloads.getDownloadManager(this);
    }

    @Override
    protected Scheduler getScheduler() {
        return null;
    }

    @Override
    protected Notification getForegroundNotification(List<Download> downloads, int notMetRequirements) {
        DownloadNotificationHelper helper = Downloads.getNotificationHelper(this);
        Intent intent = new Intent(this, Home.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return helper.buildProgressNotification(this, R.drawable.ic_p_download, pi, null, downloads, notMetRequirements);
    }
}
