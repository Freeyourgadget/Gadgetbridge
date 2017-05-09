package nodomain.freeyourgadget.gadgetbridge.test;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.Test;

import java.io.File;

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

    public LoggingTest() throws Exception {
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
            File dir = new File(dirName);
            return dir;
        }
        fail("Property " + Logging.PROP_LOGFILES_DIR + " has invalid value: " + dirName);
        return null; // not reached
    }

    @Test
    public void testToggleLogging() {
        try {
            File dir = getLogFilesDir();
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
}
