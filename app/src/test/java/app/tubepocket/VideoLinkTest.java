package app.tubepocket;
import org.junit.Test;
import static org.junit.Assert.*;

public class VideoLinkTest {
    private static final String EXPECTED = "https://www.youtube.com/watch?v=BaW_jenozKc";
    @Test public void acceptsCommonLinkFormsAndShareText() {
        for (String link : new String[]{
            "https://youtu.be/BaW_jenozKc?si=hello", "https://www.youtube.com/watch?v=BaW_jenozKc&list=123",
            "https://youtube.com/shorts/BaW_jenozKc", "https://m.youtube.com/watch?v=BaW_jenozKc&t=10",
            "https://youtube.com/live/BaW_jenozKc", "youtu.be/BaW_jenozKc", "我想保存这个视频：https://youtu.be/BaW_jenozKc。", "看这里 (https://youtu.be/BaW_jenozKc)."
        }) assertEquals(link, EXPECTED, VideoLink.normalize(link));
    }
    @Test public void rejectsSpoofedHostsAndNonVideoLinks() {
        for (String link : new String[]{
            "https://evil.youtube.com/watch?v=BaW_jenozKc", "https://youtube.com.evil.test/watch?v=BaW_jenozKc",
            "https://youtube.com@evil.test/watch?v=BaW_jenozKc", "https://evil.test/youtu.be/BaW_jenozKc",
            "https://youtube.com/playlist?list=123", "https://youtu.be/invalid", "https://youtube.com/@channel", "", "file:///etc/passwd"
        }) {
            try { VideoLink.normalize(link); fail("Accepted " + link); } catch (IllegalArgumentException expected) { }
        }
    }
    @Test public void normalizesEncodedVideoIdAndStripsTracking() { assertEquals(EXPECTED, VideoLink.normalize("https://youtube.com/watch?feature=share&v=BaW%5FjenozKc#chapter")); }
}
