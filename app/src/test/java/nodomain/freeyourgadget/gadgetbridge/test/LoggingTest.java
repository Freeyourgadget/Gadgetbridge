package nodomain.freeyourgadget.gadgetbridge.test;

import android.support.annotation.NonNull;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class LoggingTest {

    @BeforeClass
    public static void setupSuite() {
        System.setProperty("logback.configurationFile", "logback.xml");
    }

    private Logging logging = new Logging() {
        @Override
        protected String createLogDirectory() throws IOException {
            File dir = ensureLogFilesDir();
            return dir.getAbsolutePath();
        }

        @NonNull
        private File ensureLogFilesDir() throws IOException {
            return FileUtils.createTempDir("logfiles");
        }
    };

    @After
    public void tearDown() {
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
        } catch (AssertionFailedError ignored) {
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
        } catch (AssertionFailedError ex) {
            logging.debugLoggingConfiguration();
            System.err.println(System.getProperty("java.class.path"));
            throw ex;
        }
    }
}
