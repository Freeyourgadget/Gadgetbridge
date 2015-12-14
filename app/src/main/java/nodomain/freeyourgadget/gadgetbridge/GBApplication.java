package nodomain.freeyourgadget.gadgetbridge;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBConstants;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.IDSenderLookup;

/**
 * Main Application class that initializes and provides access to certain things like
 * logging and DB access.
 */
public class GBApplication extends Application {
    // Since this class must not log to slf4j, we use plain android.util.Log
    private static final String TAG = "GBApplication";
    private static GBApplication context;
    private static ActivityDatabaseHandler mActivityDatabaseHandler;
    private static final Lock dbLock = new ReentrantLock();
    private static DeviceService deviceService;
    private static SharedPreferences sharedPrefs;
    private static IDSenderLookup mIDSenderLookup = new IDSenderLookup();

    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.quit";
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_QUIT:
                    quit();
                    break;
            }
        }
    };

    private void quit() {
        GB.removeAllNotifications(this);
    }

    public GBApplication() {
        context = this;
        // don't do anything here, add it to onCreate instead
    }

    protected DeviceService createDeviceService() {
        return new GBDeviceService(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        // don't do anything here before we set up logging, otherwise
        // slf4j may be implicitly initialized before we properly configured it.
        setupLogging();

        setupExceptionHandler();
//        For debugging problems with the logback configuration
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//         print logback's internal status
//        StatusPrinter.print(lc);
//        Logger logger = LoggerFactory.getLogger(GBApplication.class);

        deviceService = createDeviceService();
        GB.environment = GBEnvironment.createDeviceEnvironment();
        mActivityDatabaseHandler = new ActivityDatabaseHandler(context);
        loadBlackList();

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_QUIT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

// for testing DB stuff
//        SQLiteDatabase db = mActivityDatabaseHandler.getWritableDatabase();
//        db.close();
    }

    private void setupExceptionHandler() {
        LoggingExceptionHandler handler = new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    public static boolean isFileLoggingEnabled() {
        return sharedPrefs.getBoolean("log_to_file", false);
    }

    private void setupLogging() {
        if (isFileLoggingEnabled()) {
            try {
                File dir = FileUtils.getExternalFilesDir();
                // used by assets/logback.xml since the location cannot be statically determined
                System.setProperty("GB_LOGFILES_DIR", dir.getAbsolutePath());
                getLogger().info("Gadgetbridge version: " + BuildConfig.VERSION_NAME);
            } catch (IOException ex) {
                Log.e("GBApplication", "External files dir not available, cannot log to file", ex);
                removeFileLogger();
            }
        } else {
            removeFileLogger();
        }
    }

    private void removeFileLogger() {
        try {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.detachAppender("FILE");
        } catch (Throwable ex) {
            Log.e("GBApplication", "Error removing logger FILE appender", ex);
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(GBApplication.class);
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Returns the facade for talking to devices. Devices are managed by
     * an Android Service and this facade provides access to its functionality.
     *
     * @return the facade for talking to the service/devices.
     */
    public static DeviceService deviceService() {
        return deviceService;
    }

    /**
     * Returns the DBHandler instance for reading/writing or throws GBException
     * when that was not successful
     * If acquiring was successful, callers must call #releaseDB when they
     * are done (from the same thread that acquired the lock!
     *
     * @return the DBHandler
     * @throws GBException
     * @see #releaseDB()
     */
    public static DBHandler acquireDB() throws GBException {
        try {
            if (dbLock.tryLock(30, TimeUnit.SECONDS)) {
                return mActivityDatabaseHandler;
            }
        } catch (InterruptedException ex) {
            Log.i(TAG, "Interrupted while waiting for DB lock");
        }
        throw new GBException("Unable to access the database.");
    }

    /**
     * Releases the database lock.
     *
     * @throws IllegalMonitorStateException if the current thread is not owning the lock
     * @see #acquireDB()
     */
    public static void releaseDB() {
        dbLock.unlock();
    }

    public static boolean isRunningOnKitkatOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static boolean isRunningLollipopOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static HashSet<String> blacklist = null;

    private static void loadBlackList() {
        blacklist = (HashSet<String>) sharedPrefs.getStringSet("package_blacklist", null);
        if (blacklist == null) {
            blacklist = new HashSet<>();
        }
    }

    private static void saveBlackList() {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (blacklist.isEmpty()) {
            editor.putStringSet("package_blacklist", null);
        } else {
            editor.putStringSet("package_blacklist", blacklist);
        }
        editor.apply();
    }

    public static void addToBlacklist(String packageName) {
        if (!blacklist.contains(packageName)) {
            blacklist.add(packageName);
            saveBlackList();
        }
    }

    public static synchronized void removeFromBlacklist(String packageName) {
        blacklist.remove(packageName);
        saveBlackList();
    }

    /**
     * Deletes the entire Activity database and recreates it with empty tables.
     *
     * @return true on successful deletion
     */
    public static synchronized boolean deleteActivityDatabase() {
        if (mActivityDatabaseHandler != null) {
            mActivityDatabaseHandler.close();
            mActivityDatabaseHandler = null;
        }
        boolean result = getContext().deleteDatabase(DBConstants.DATABASE_NAME);
        mActivityDatabaseHandler = new ActivityDatabaseHandler(getContext());
        return result;
    }

    public static IDSenderLookup getIDSenderLookup() {
        return mIDSenderLookup;
    }
}
