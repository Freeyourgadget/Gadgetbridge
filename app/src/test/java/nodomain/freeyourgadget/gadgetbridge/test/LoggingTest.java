package nodomain.freeyourgadget.gadgetbridge.test;

import org.junit.After;
import org.junit.Test;

import java.io.File;

import androidx.annotation.NonNull;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests dynamic enablement and disablement of file appenders.
 */
public class LoggingTest extends TestBase {

    public LoggingTest() {
    }

    private Logging logging = GBApplication.getLogging();

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
        assertTrue(FileUtils.deleteRecursively(getLogFilesDir()));
    }

    @NonNull
    private File getLogFilesDir() {
        String dirName = System.getProperty(Logging.PROP_LOGFILES_DIR);
        if (dirName != null && dirName.length() > 5) {
            return new File(dirName);
        }
        fail("Property " + Logging.PROP_LOGFILES_DIR + " has invalid value: " + dirName);
        return null; // not reached
    }

    @Test
    public void testToggleLogging() {
        try {
            getLogFilesDir();
        } catch (AssertionError ignored) {
            // expected, as not yet set up
        }

        try {
            logging.setupLogging(true);
            File dir = getLogFilesDir();
            assertEquals(1, dir.list().length);
            assertNotNull(logging.getFileLogger());
            assertTrue(logging.getFileLogger().isStarted());

            logging.setupLogging(false);
            assertNotNull(logging.getFileLogger());
            assertFalse(logging.getFileLogger().isStarted());

            logging.setupLogging(true);
            assertNotNull(logging.getFileLogger());
            assertTrue(logging.getFileLogger().isStarted());
        } catch (AssertionError ex) {
            logging.debugLoggingConfiguration();
            System.err.println(System.getProperty("java.class.path"));
            throw ex;
        }
    }

    @Test
    public void testLogFormat() {
        String tempOut = Logging.formatBytes(new byte[] {0xa});
        assertEquals("0x0a", tempOut);

        tempOut = Logging.formatBytes(new byte[] {0xa, 1, (byte) 255});
        assertEquals("0x0a 0x01 0xff", tempOut);
    }
}
