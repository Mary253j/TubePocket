package app.tubepocket;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import com.yausername.youtubedl_android.YoutubeDL;

public final class PocketApp extends Application {
    public static PocketApp instance;
    public JobStore jobs;
    @Override public void onCreate() {
        super.onCreate(); instance = this; jobs = new JobStore(this);
        new CookieStore(getNoBackupFilesDir()).cleanupStaleCopies();
        getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("downloads", "视频下载", NotificationManager.IMPORTANCE_LOW));
    }
    public void cancel(String id) {
        if (jobs.cancel(id)) Engine.cancel(id);
    }
}
