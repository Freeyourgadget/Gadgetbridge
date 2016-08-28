package nodomain.freeyourgadget.gadgetbridge.test;

import android.support.annotation.NonNull;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests dynamic enablement and disablement of file appenders.
 * Test is currently disabled because logback-android does not work
 * inside a plain junit test.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 19)
// need sdk 19 because "WITHOUT ROWID" is not supported in robolectric/sqlite4java
public class LoggingTest {

    private static File logFilesDir;

    public LoggingTest() throws Exception {
    }

    @BeforeClass
    public static void setupSuite() throws Exception {
        logFilesDir = FileUtils.createTempDir("logfiles");
        System.setProperty(Logging.PROP_LOGFILES_DIR, logFilesDir.getAbsolutePath());
        File workingDir = new File(System.getProperty("user.dir"));
        File configFile = new File(workingDir, "src/main/assets/logback.xml");
        System.out.println(configFile.getAbsolutePath());
        System.setProperty("logback.configurationFile", configFile.getAbsolutePath());
    }

    private Logging logging = new Logging() {
        @Override
        protected String createLogDirectory() throws IOException {
            return logFilesDir.getAbsolutePath();
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
