package app.tubepocket;

import org.junit.Test;
import static org.junit.Assert.*;

public class ErrorMessagesTest {
    @Test public void botChallengeGetsVerificationGuidanceRatherThanGenericFormatError() {
        String error = "ERROR: [youtube] BaW_jenozKc: Sign in to confirm you’re not a bot. Use --cookies for authentication.";
        assertTrue(ErrorMessages.needsVerification(error)); assertTrue(ErrorMessages.needsSession(error));
        assertTrue(ErrorMessages.friendly(new IOExceptionForTest(error)).contains("人机验证"));
        assertFalse(ErrorMessages.needsVerification("Requested format is not available"));
    }
    @Test public void credentialBearingDetailsAreHidden() {
        String safe = ErrorMessages.safeDetails("ERROR: verification required\nCookie: SID=FAKE_SECRET\nAuthorization: FAKE_TOKEN\nWARNING: skipping cookie file entry FAKE_VALUE");
        assertTrue(safe.contains("verification required")); assertFalse(safe.contains("FAKE_SECRET")); assertFalse(safe.contains("FAKE_TOKEN")); assertFalse(safe.contains("FAKE_VALUE"));
    }
    private static final class IOExceptionForTest extends Exception { IOExceptionForTest(String message) { super(message); } }
}
