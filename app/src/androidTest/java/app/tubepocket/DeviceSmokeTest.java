package app.tubepocket;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.yausername.youtubedl_android.*;
import org.junit.*;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

/** No remote media requests: native runtime, local extraction, and public-storage smoke tests. */
@RunWith(AndroidJUnit4.class)
public class DeviceSmokeTest {
    private Context context;
    @Before public void setup() throws Exception { context = InstrumentationRegistry.getInstrumentation().getTargetContext(); Engine.init(context); }

    @Test public void bundledPythonAndJavaScriptRuntimeExecute() throws Exception {
        YoutubeDLRequest request = new YoutubeDLRequest(Collections.emptyList()); request.addOption("--version");
        assertEquals(BuildConfig.BUNDLED_ENGINE, YoutubeDL.getInstance().execute(request).getOut().trim());
        Process js = new ProcessBuilder(context.getApplicationInfo().nativeLibraryDir + "/libqjs.so", "--eval", "print(21+21)").redirectErrorStream(true).start();
        String output = read(js.getInputStream()); assertEquals(output, 0, js.waitFor()); assertEquals("42", output.trim());
    }

    @Test public void ffmpegLocalDownloadAndPublicStorageWork() throws Exception {
        File directory = new File(context.getCacheDir(), "device-smoke"); assertTrue(directory.isDirectory() || directory.mkdirs());
        File fixture = new File(directory, "fixture.mp4"); File downloaded = new File(directory, "download.mp4");
        ProcessBuilder ffmpeg = new ProcessBuilder(context.getApplicationInfo().nativeLibraryDir + "/libffmpeg.so", "-y", "-f", "lavfi", "-i", "color=c=green:s=160x90:d=0.6", "-f", "lavfi", "-i", "sine=frequency=440:duration=0.6", "-c:v", "mpeg4", "-c:a", "aac", "-shortest", fixture.getAbsolutePath()).redirectErrorStream(true);
        String packages = context.getNoBackupFilesDir() + "/youtubedl-android/packages/";
        ffmpeg.environment().put("LD_LIBRARY_PATH", packages + "ffmpeg/usr/lib:" + packages + "python/usr/lib");
        Process process = ffmpeg.start(); String output = read(process.getInputStream()); assertEquals(output, 0, process.waitFor()); assertTrue(fixture.length() > 1024);
        YoutubeDLRequest request = new YoutubeDLRequest(Uri.fromFile(fixture).toString());
        request.addOption("--enable-file-urls").addOption("--no-playlist").addOption("--force-overwrites").addOption("-o", downloaded.getAbsolutePath());
        YoutubeDL.getInstance().execute(request); assertEquals(fixture.length(), downloaded.length());
        assertArrayEquals(bytes(new FileInputStream(fixture)), bytes(new FileInputStream(downloaded)));
        if (Build.VERSION.SDK_INT >= 29) {
            Job job = new Job(UUID.randomUUID().toString(), "https://www.youtube.com/watch?v=BaW_jenozKc", "本地引擎验证", 480);
            Uri saved = VideoStorage.publish(context, job, downloaded);
            try {
                try (Cursor cursor = context.getContentResolver().query(saved, new String[]{MediaStore.MediaColumns.IS_PENDING, MediaStore.MediaColumns.RELATIVE_PATH}, null, null, null)) {
                    assertNotNull(cursor); assertTrue(cursor.moveToFirst()); assertEquals(0, cursor.getInt(0)); assertEquals("Download/TubePocket/", cursor.getString(1));
                }
                assertArrayEquals(bytes(new FileInputStream(downloaded)), bytes(context.getContentResolver().openInputStream(saved)));
            } finally { context.getContentResolver().delete(saved, null, null); }
        }
        VideoStorage.deleteTree(directory);
    }

    @Test public void cancellationAndRestartRecoveryArePersistent() {
        Context isolated = new ContextWrapper(context) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) { return super.getSharedPreferences("smoke-" + name, mode); }
        };
        isolated.getSharedPreferences("downloads", Context.MODE_PRIVATE).edit().clear().commit();
        JobStore store = new JobStore(isolated);
        Job cancel = new Job("cancel", "https://www.youtube.com/watch?v=BaW_jenozKc", "Cancel", 720); store.add(cancel);
        assertTrue(store.cancel("cancel")); assertFalse(store.beginSaving("cancel")); assertEquals("cancelled", store.get("cancel").state);
        Job running = new Job("running", cancel.url, "Interrupted", 1080); running.state = "running"; store.add(running);
        JobStore restarted = new JobStore(isolated); assertEquals("interrupted", restarted.get("running").state);
        assertEquals("cancelled", restarted.get("cancel").state);
        isolated.getSharedPreferences("downloads", Context.MODE_PRIVATE).edit().clear().commit();
    }
    private static String read(InputStream in) throws IOException { return new String(bytes(in), java.nio.charset.StandardCharsets.UTF_8); }
    private static byte[] bytes(InputStream in) throws IOException {
        try (InputStream source = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) { byte[] buffer = new byte[8192]; int n; while ((n = source.read(buffer)) != -1) out.write(buffer, 0, n); return out.toByteArray(); }
    }
}
