package app.tubepocket;

import com.yausername.youtubedl_android.YoutubeDLRequest;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CookieStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static InputStream fake(String value) {
        return new ByteArrayInputStream((".youtube.com\tTRUE\t/\tTRUE\t0\tSID\t" + value + "\n").getBytes(StandardCharsets.UTF_8));
    }
    @Test public void engineReceivesPrivateCopyAndCannotMutateImportedSession() throws Exception {
        CookieStore store = new CookieStore(temporary.getRoot());
        assertEquals(1, store.importFrom(fake("FAKE_ORIGINAL")));
        YoutubeDLRequest request = new YoutubeDLRequest("https://www.youtube.com/watch?v=BaW_jenozKc");
        File copy;
        try (CookieStore.Session session = store.attach(request)) {
            copy = new File(request.getOption("--cookies")); assertTrue(copy.isFile());
            assertTrue(request.buildCommand().contains(copy.getAbsolutePath()));
            Files.write(copy.toPath(), "FAKE_CHANGED_BY_ENGINE".getBytes(StandardCharsets.UTF_8));
            try { store.clear(); fail("Cleared active session"); } catch (IOException expected) { }
            YoutubeDLRequest concurrent = new YoutubeDLRequest("https://www.youtube.com/");
            try (CookieStore.Session other = store.attach(concurrent)) {
                File separate = new File(concurrent.getOption("--cookies")); assertNotEquals(copy, separate);
                assertTrue(new String(Files.readAllBytes(separate.toPath()), StandardCharsets.UTF_8).contains("FAKE_ORIGINAL"));
            }
        }
        assertFalse(copy.exists()); store.clear(); assertFalse(store.hasSession());
    }
    @Test public void invalidReplacementPreservesExistingCredentials() throws Exception {
        CookieStore store = new CookieStore(temporary.getRoot()); store.importFrom(fake("FAKE_KEEP"));
        try { store.importFrom(new ByteArrayInputStream("bad export".getBytes(StandardCharsets.UTF_8))); fail(); }
        catch (IllegalArgumentException expected) { }
        YoutubeDLRequest request = new YoutubeDLRequest("https://www.youtube.com/");
        try (CookieStore.Session session = store.attach(request)) {
            assertTrue(new String(Files.readAllBytes(new File(request.getOption("--cookies")).toPath()), StandardCharsets.UTF_8).contains("FAKE_KEEP"));
        }
    }
    @Test public void startupCleansStaleCopiesAndAnonymousModeHasNoCookieArgument() throws Exception {
        CookieStore store = new CookieStore(temporary.getRoot());
        YoutubeDLRequest anonymous = new YoutubeDLRequest("https://www.youtube.com/");
        try (CookieStore.Session session = store.attach(anonymous)) { assertFalse(anonymous.hasOption("--cookies")); }
        store.importFrom(fake("FAKE_RETAIN"));
        File stale = new File(new File(temporary.getRoot(), "youtube-session"), "request-stale.txt");
        Files.write(stale.toPath(), "FAKE_STALE".getBytes(StandardCharsets.UTF_8));
        store.cleanupStaleCopies(); assertFalse(stale.exists()); assertTrue(store.hasSession());
    }
}
