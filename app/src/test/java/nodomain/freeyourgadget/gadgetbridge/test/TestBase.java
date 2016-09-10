package nodomain.freeyourgadget.gadgetbridge.test;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import java.io.File;

import ch.qos.logback.classic.util.ContextInitializer;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.Logging;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 19)
// need sdk 19 because "WITHOUT ROWID" is not supported in robolectric/sqlite4java
public abstract class TestBase {
    protected static File logFilesDir;

    protected GBApplication app = (GBApplication) RuntimeEnvironment.application;
    protected DaoSession daoSession;
    protected DBHandler dbHandler;

    // Make sure logging is set up for all testcases, so that we can debug problems
    @BeforeClass
    public static void setupSuite() throws Exception {
        // print everything going to android.util.Log to System.out
        ShadowLog.stream = System.out;

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
        assertNotNull(app);
        assertNotNull(getContext());
// doesn't work with Robolectric yet
//        dbHandler = GBApplication.acquireDB();
//        daoSession = dbHandler.getDaoSession();
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(app, null, null);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        daoSession = new DaoMaster(db).newSession();
        assertNotNull(daoSession);
    }

    @After
    public void tearDown() throws Exception {
//        GBApplication.releaseDB();
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
