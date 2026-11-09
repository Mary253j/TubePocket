package app.tubepocket;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.*;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import kotlin.Unit;

public final class DownloadService extends Service {
    public static final String ENQUEUE = "enqueue", CANCEL = "cancel";
    private static volatile String executingId;
    public static boolean isExecuting(String id) { return id != null && id.equals(executingId); }
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor();
    private final ArrayDeque<String> queue = new ArrayDeque<>();
    private final Set<String> pending = new HashSet<>();
    private String activeId;
    private volatile boolean destroyed;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        String id = intent.getStringExtra("id");
        if (CANCEL.equals(intent.getAction())) {
            PocketApp.instance.cancel(id);
            if (activeId == null && queue.isEmpty()) stopSelf();
            return START_NOT_STICKY;
        }
        Notification notification = notification("准备下载", "正在准备下载任务", 0, id);
        if (Build.VERSION.SDK_INT >= 29) startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        else startForeground(1, notification);
        Job job = PocketApp.instance.jobs.get(id);
        if (job != null && job.state.equals("queued") && pending.add(id)) queue.add(id);
        if (activeId == null) next();
        return START_NOT_STICKY;
    }

    private void next() {
        if (destroyed) return;
        String id = queue.poll();
        if (id == null) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return; }
        activeId = id;
        executingId = id;
        worker.execute(() -> {
            try { download(id); }
            finally { main.post(() -> { pending.remove(id); activeId = null; executingId = null; next(); }); }
        });
    }

    private void download(String id) {
        Job job = PocketApp.instance.jobs.get(id);
        if (job == null || !job.state.equals("queued")) return;
        PocketApp.instance.jobs.edit(id, j -> { if (j.state.equals("queued")) { j.state = "running"; j.detail = "正在准备下载引擎"; j.rawError = ""; } });
        PowerManager.WakeLock wakeLock = getSystemService(PowerManager.class).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TubePocket:download");
        wakeLock.acquire(6 * 60 * 60 * 1000L);
        ScheduledFuture<?> cancellation = watchdog.scheduleWithFixedDelay(() -> {
            Job current = PocketApp.instance.jobs.get(id);
            if (current != null && current.state.equals("cancelled")) Engine.cancel(id);
        }, 0, 500, TimeUnit.MILLISECONDS);
        File work = new File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), id);
        Engine.lock.readLock().lock();
        try {
            Engine.init(this);
            if (!work.isDirectory() && !work.mkdirs()) throw new IOException("无法创建临时文件夹");
            if (new StatFs(work.getAbsolutePath()).getAvailableBytes() < 50L * 1024 * 1024) throw new IOException("No space left");
            if (cancelled(id)) return;
            YoutubeDLRequest request = new YoutubeDLRequest(VideoLink.normalize(job.url));
            request.addOption("--no-playlist").addOption("--no-warnings").addOption("--newline")
                .addOption("--socket-timeout", 20).addOption("--retries", 3).addOption("--fragment-retries", 3)
                .addOption("--concurrent-fragments", 2).addOption("--continue").addOption("--no-mtime")
                .addOption("-f", DownloadPolicy.format(job.height)).addOption("-S", "res,vcodec:h264,acodec:aac").addOption("--merge-output-format", "mp4")
                .addOption("--remux-video", "mp4").addOption("-o", new File(work, "video.%(ext)s").getAbsolutePath());
            AtomicLong last = new AtomicLong();
            update(id, "下载视频与音轨", 0);
            Engine.execute(this, request, id, (progress, eta, line) -> {
                long now = SystemClock.elapsedRealtime();
                if (now - last.get() >= 800) {
                    last.set(now);
                    int percent = Math.min(99, Math.max(0, (int)(float)progress));
                    String detail = line.contains("[Merger]") || line.contains("[VideoRemuxer]") ? "正在合并视频与音轨" : "下载视频与音轨 · " + percent + "%" + (eta > 0 ? " · 剩余约 " + eta + " 秒" : "");
                    update(id, detail, percent);
                }
                return Unit.INSTANCE;
            });
            if (cancelled(id)) return;
            File source = new File(work, "video.mp4");
            if (!source.isFile() || source.length() == 0) throw new IOException("视频文件未生成，请更新解析引擎后重试");
            if (!PocketApp.instance.jobs.beginSaving(id)) return;
            show(notification(job.title, "正在保存到系统下载文件夹", 99, null));
            Uri uri = VideoStorage.publish(this, job, source);
            PocketApp.instance.jobs.edit(id, j -> { j.state = "completed"; j.progress = 100; j.detail = "已保存 · Download/TubePocket"; j.uri = uri.toString(); });
            VideoStorage.deleteTree(work);
        } catch (Exception e) {
            if (!cancelled(id)) PocketApp.instance.jobs.edit(id, j -> {
                j.state = Thread.currentThread().isInterrupted() ? "interrupted" : "failed";
                j.detail = Thread.currentThread().isInterrupted() ? "下载被中断，点击重试" : Engine.friendly(e);
                j.rawError = ErrorMessages.safeDetails(e.getMessage());
            });
        } finally {
            Engine.lock.readLock().unlock(); cancellation.cancel(false);
            if (wakeLock.isHeld()) wakeLock.release();
        }
    }
    private boolean cancelled(String id) { Job job = PocketApp.instance.jobs.get(id); return destroyed || job == null || job.state.equals("cancelled"); }
    private void update(String id, String detail, int progress) {
        PocketApp.instance.jobs.edit(id, j -> { if (j.state.equals("running")) { j.detail = detail; j.progress = progress; } });
        Job job = PocketApp.instance.jobs.get(id);
        if (job != null && job.state.equals("running")) show(notification(job.title, detail, progress, id));
    }
    private void show(Notification notification) { main.post(() -> { if (!destroyed) getSystemService(NotificationManager.class).notify(1, notification); }); }
    private Notification notification(String title, String detail, int progress, String id) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this, "downloads").setSmallIcon(R.drawable.ic_download)
            .setContentTitle(title).setContentText(detail).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0).setCategory(Notification.CATEGORY_PROGRESS);
        if (id != null) {
            Intent cancel = new Intent(this, DownloadService.class).setAction(CANCEL).putExtra("id", id);
            PendingIntent action = PendingIntent.getService(this, 1, cancel, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            builder.addAction(new Notification.Action.Builder(null, "取消", action).build());
        }
        return builder.build();
    }
    @Override public void onTimeout(int startId, int fgsType) {
        for (String id : new ArrayList<>(pending)) PocketApp.instance.cancel(id);
        stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }
    @Override public void onDestroy() {
        destroyed = true;
        executingId = null;
        for (String id : new ArrayList<>(pending)) {
            Engine.cancel(id);
            PocketApp.instance.jobs.edit(id, j -> {
                if (j.state.equals("running") || j.state.equals("queued") || j.state.equals("saving")) { j.state = "interrupted"; j.detail = "下载被系统中断，点击重试"; }
            });
        }
        worker.shutdownNow(); watchdog.shutdownNow(); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
