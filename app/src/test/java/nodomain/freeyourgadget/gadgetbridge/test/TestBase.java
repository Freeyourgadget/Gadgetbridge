package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;

import ch.qos.logback.classic.util.ContextInitializer;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBEnvironment;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertNotNull;

/**
 * Base class for all testcases in Gadgetbridge that are supposed to run locally
 * with robolectric.
 *
 * Important: To run them, create a run configuration and execute them in the Gadgetbridge/app/
 * directory.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 19)
// need sdk 19 because "WITHOUT ROWID" is not supported in robolectric/sqlite4java
public abstract class TestBase {
    protected static File logFilesDir;

    protected GBApplication app = (GBApplication) RuntimeEnvironment.application;
    protected DaoSession daoSession;
    protected DBHandler dbHandler;

    // Make sure logging is set up for all testcases, so that we can debug problems
    @BeforeClass
    public static void setupSuite() throws Exception {
        GBEnvironment.setupEnvironment(GBEnvironment.createLocalTestEnvironment());

        // print everything going to android.util.Log to System.out
        System.setProperty("robolectric.logging", "stdout");

        // properties might be preconfigured in build.gradle because of test ordering problems
        String logDir = System.getProperty(Logging.PROP_LOGFILES_DIR);
        if (logDir != null) {
            logFilesDir = new File(logDir);
        } else {
            logFilesDir = FileUtils.createTempDir("logfiles");
            System.setProperty(Logging.PROP_LOGFILES_DIR, logFilesDir.getAbsolutePath());
        }

        if (System.getProperty(ContextInitializer.CONFIG_FILE_PROPERTY) == null) {
            File workingDir = new File(System.getProperty("user.dir"));
            File configFile = new File(workingDir, "src/main/assets/logback.xml");
            System.out.println(configFile.getAbsolutePath());
            System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
        }
    }

    @Before
    public void setUp() throws Exception {
        app = (GBApplication) RuntimeEnvironment.application;
        assertNotNull(app);
        assertNotNull(getContext());
        app.setupDatabase();
        dbHandler = GBApplication.acquireDB();
        daoSession = dbHandler.getDaoSession();
        assertNotNull(daoSession);
    }

    @After
    public void tearDown() throws Exception {
        dbHandler.closeDb();
        GBApplication.releaseDB();
    }

    protected GBDevice createDummyGDevice(String macAddress) {
        GBDevice dummyGBDevice = new GBDevice(macAddress, "Testie", DeviceType.TEST);
        dummyGBDevice.setFirmwareVersion("1.2.3");
        dummyGBDevice.setModel("4.0");
        return dummyGBDevice;
    }

    protected Context getContext() {
        return app;
    }
}
