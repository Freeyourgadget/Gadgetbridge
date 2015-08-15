package nodomain.freeyourgadget.gadgetbridge;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import nodomain.freeyourgadget.gadgetbridge.database.ActivityDatabaseHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class GBApplication extends Application {
    // Since this class must not log to slf4j, we use plain android.util.Log
    private static final String TAG = "GBApplication";
    private static GBApplication context;
    private static ActivityDatabaseHandler mActivityDatabaseHandler;
    private static final Lock dbLock = new ReentrantLock();

    public GBApplication() {
        context = this;
        // don't do anything here, add it to onCreate instead
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // don't do anything here before we set up logging, otherwise
        // slf4j may be implicitly initialized before we properly configured it.
        setupLogging();
//        For debugging problems with the logback configuration
//        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
//         print logback's internal status
//        StatusPrinter.print(lc);
//        Logger logger = LoggerFactory.getLogger(GBApplication.class);

        mActivityDatabaseHandler = new ActivityDatabaseHandler(context);
// for testing DB stuff
//        SQLiteDatabase db = mActivityDatabaseHandler.getWritableDatabase();
//        db.close();
    }

    public static boolean isFileLoggingEnabled() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        return prefs.getBoolean("log_to_file", false);
    }

    private void setupLogging() {
        if (isFileLoggingEnabled()) {
            try {
                File dir = FileUtils.getExternalFilesDir();
                // used by assets/logback.xml since the location cannot be statically determined
                System.setProperty("GB_LOGFILES_DIR", dir.getAbsolutePath());
            } catch (IOException ex) {
                Log.e("GBApplication", "External files dir not available, cannot log to file, ex");
                System.setProperty("GB_LOGFILES_DIR", "/dev/null");
            }
        } else {
            try {
                ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
                root.detachAppender("FILE");
            } catch (Throwable ex) {
                System.out.println("Error removing logger FILE appender");
                ex.printStackTrace();
            }
        }
    }

    public static Context getContext() {
        return context;
    }

    /**
     * Returns the DBHandler instance for reading/writing or throws GBException
     * when that was not successful
     * If acquiring was successful, callers must call #releaseDB when they
     * are done (from the same thread that acquired the lock!
     * @return the DBHandler
     * @see #releaseDB()
     * @throws GBException
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
     * @throws IllegalMonitorStateException if the current thread is not owning the lock
     * @see #acquireDB()
     */
    public static void releaseDB() {
        dbLock.unlock();
    }

    public static boolean isRunningLollipopOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }
}
