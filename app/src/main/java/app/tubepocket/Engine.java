package app.tubepocket;

import android.content.Context;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.YoutubeDLResponse;
import com.yausername.ffmpeg.FFmpeg;
import kotlin.Unit;
import kotlin.jvm.functions.Function3;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class Engine {
    public static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private static boolean ready;
    public static void cancel(String id) {
        if (id == null || id.isEmpty()) return;
        try { YoutubeDL.getInstance().destroyProcessById(id); }
        catch (RuntimeException ignored) { /* The library may concurrently remove a finished process. */ }
    }
    public static synchronized void init(Context context) throws Exception {
        if (ready) return;
        YoutubeDL.getInstance().init(context.getApplicationContext());
        FFmpeg.getInstance().init(context.getApplicationContext());
        ready = true;
    }
    public static YoutubeDLResponse execute(Context context, YoutubeDLRequest request, String processId, Function3<Float, Long, String, Unit> callback) throws Exception {
        try (CookieStore.Session session = new CookieStore(context.getNoBackupFilesDir()).attach(request)) {
            return YoutubeDL.getInstance().execute(request, processId, callback);
        }
    }
    public static String friendly(Throwable error) { return ErrorMessages.friendly(error); }
    private Engine() { }
}
