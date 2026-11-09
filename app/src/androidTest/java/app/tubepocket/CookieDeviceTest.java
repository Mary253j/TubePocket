package app.tubepocket;

import android.content.*;
import android.net.Uri;
import android.system.Os;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

@RunWith(AndroidJUnit4.class)
public class CookieDeviceTest {
    @Test public void bundledEngineReadsIsolatedJarAndCleanupPreservesImportedFile() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext(); Engine.init(context);
        File directory = new File(context.getCacheDir(), "cookie-device-fixture"); assertTrue(directory.isDirectory() || directory.mkdirs());
        Context isolated = new ContextWrapper(context) { @Override public File getNoBackupFilesDir() { return directory; } };
        CookieStore store = new CookieStore(directory);
        // Dummy values only. Never reads, exports, or overwrites the real application's session.
        String fake = ".youtube.com\tTRUE\t/\tTRUE\t0\tSID\tFAKE_DEVICE_TEST\n";
        File canonical = new File(directory, "youtube-session/cookies.txt");
        try {
            assertEquals(1, store.importFrom(new ByteArrayInputStream(fake.getBytes(StandardCharsets.UTF_8))));
            byte[] original = Files.readAllBytes(canonical.toPath()); assertEquals(0, Os.stat(canonical.getAbsolutePath()).st_mode & 0077);
            File info = new File(directory, "fixture.info.json");
            JSONObject metadata = new JSONObject().put("id", "cookie-fixture").put("title", "Local fixture").put("ext", "mp4")
                    .put("url", Uri.fromFile(new File(directory, "not-requested.mp4")).toString()).put("extractor", "generic").put("extractor_key", "Generic");
            Files.write(info.toPath(), metadata.toString().getBytes(StandardCharsets.UTF_8));
            YoutubeDLRequest request = new YoutubeDLRequest(Collections.emptyList());
            request.addOption("--enable-file-urls").addOption("--load-info-json", info.getAbsolutePath()).addOption("--skip-download").addOption("--dump-single-json");
            JSONObject result = new JSONObject(Engine.execute(isolated, request, "cookie-device", null).getOut());
            assertEquals("cookie-fixture", result.getString("id")); assertArrayEquals(original, Files.readAllBytes(canonical.toPath()));
            assertNotNull(request.getOption("--cookies")); assertFalse(new File(request.getOption("--cookies")).exists());
            store.clear(); assertFalse(store.hasSession());
        } finally { VideoStorage.deleteTree(directory); }
    }
}
