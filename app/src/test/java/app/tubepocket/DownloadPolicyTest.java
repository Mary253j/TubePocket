package app.tubepocket;
import org.junit.Test;
import static org.junit.Assert.*;

public class DownloadPolicyTest {
    @Test public void capsEveryVideoFallback() {
        for (String fallback : DownloadPolicy.format(720).split("/")) assertTrue(fallback, fallback.contains("[height<=720]"));
        assertFalse(DownloadPolicy.format(0).contains("height<="));
    }
    @Test public void protectsPublishingAndCompletedFilesFromCancellation() {
        assertTrue(DownloadPolicy.cancellable("queued")); assertTrue(DownloadPolicy.cancellable("running"));
        assertFalse(DownloadPolicy.cancellable("saving")); assertFalse(DownloadPolicy.cancellable("completed"));
        assertFalse(DownloadPolicy.retryable("running")); assertTrue(DownloadPolicy.retryable("interrupted"));
    }
}
