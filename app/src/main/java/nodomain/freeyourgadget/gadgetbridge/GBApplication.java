package nodomain.freeyourgadget.gadgetbridge;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;

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
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

//import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothConnectReceiver;

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
    private static final String PREFS_VERSION = "shared_preferences_version";
    //if preferences have to be migrated, increment the following and add the migration logic in migratePrefs below; see http://stackoverflow.com/questions/16397848/how-can-i-migrate-android-preferences-with-a-new-version
    private static final int CURRENT_PREFS_VERSION = 2;
    private static LimitedQueue mIDSenderLookup = new LimitedQueue(16);
    private static Prefs prefs;
    private static GBPrefs gbPrefs;
    /**
     * Note: is null on Lollipop and Kitkat
     */
    private static NotificationManager notificationManager;

    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.quit";
    private static Logging logging = new Logging() {
        @Override
        protected String createLogDirectory() throws IOException {
            File dir = FileUtils.getExternalFilesDir();
            return dir.getAbsolutePath();
        }
    };
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
        prefs = new Prefs(sharedPrefs);
        gbPrefs = new GBPrefs(prefs);

        // don't do anything here before we set up logging, otherwise
        // slf4j may be implicitly initialized before we properly configured it.
        setupLogging(isFileLoggingEnabled());

        if (getPrefsFileVersion() != CURRENT_PREFS_VERSION) {
            migratePrefs(getPrefsFileVersion());
        }

        setupExceptionHandler();

        deviceService = createDeviceService();
        GB.environment = GBEnvironment.createDeviceEnvironment();
        mActivityDatabaseHandler = new ActivityDatabaseHandler(context);
        loadBlackList();

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ACTION_QUIT);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        if (isRunningMarshmallowOrLater()) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        }

// for testing DB stuff
//        SQLiteDatabase db = mActivityDatabaseHandler.getWritableDatabase();
//        db.close();
    }

    public static void setupLogging(boolean enabled) {
        logging.setupLogging(enabled);
    }

    private void setupExceptionHandler() {
        LoggingExceptionHandler handler = new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    public static boolean isFileLoggingEnabled() {
        return prefs.getBoolean("log_to_file", false);
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

    public static boolean isRunningLollipopOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    }

    public static boolean isRunningMarshmallowOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private static boolean isPrioritySender(int prioritySenders, String number) {
        if (prioritySenders == Policy.PRIORITY_SENDERS_ANY) {
            return true;
        } else {
            Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            String[] projection = new String[]{PhoneLookup._ID, PhoneLookup.STARRED};
            Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
            boolean exists = false;
            int starred = 0;
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    exists = true;
                    starred = cursor.getInt(cursor.getColumnIndexOrThrow(PhoneLookup.STARRED));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (prioritySenders == Policy.PRIORITY_SENDERS_CONTACTS && exists) {
                return true;
            } else if (prioritySenders == Policy.PRIORITY_SENDERS_STARRED && starred == 1) {
                return true;
            }
            return false;
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static boolean isPriorityNumber(int priorityType, String number) {
        NotificationManager.Policy notificationPolicy = notificationManager.getNotificationPolicy();
        if(priorityType == Policy.PRIORITY_CATEGORY_MESSAGES) {
            if ((notificationPolicy.priorityCategories & Policy.PRIORITY_CATEGORY_MESSAGES) == Policy.PRIORITY_CATEGORY_MESSAGES) {
                return isPrioritySender(notificationPolicy.priorityMessageSenders, number);
            }
        } else if (priorityType == Policy.PRIORITY_CATEGORY_CALLS) {
            if ((notificationPolicy.priorityCategories & Policy.PRIORITY_CATEGORY_CALLS) == Policy.PRIORITY_CATEGORY_CALLS) {
                return isPrioritySender(notificationPolicy.priorityCallSenders, number);
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static int getGrantedInterruptionFilter() {
        if (prefs.getBoolean("notification_filter", false) && GBApplication.isRunningMarshmallowOrLater()) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                return notificationManager.getCurrentInterruptionFilter();
            }
        }
        return NotificationManager.INTERRUPTION_FILTER_ALL;
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

    private int getPrefsFileVersion() {
        try {
            return Integer.parseInt(sharedPrefs.getString(PREFS_VERSION, "0")); //0 is legacy
        } catch (Exception e) {
            //in version 1 this was an int
            return 1;
        }
    }

    private void migratePrefs(int oldVersion) {
        SharedPreferences.Editor editor = sharedPrefs.edit();
        switch (oldVersion) {
            case 0:
                String legacyGender = sharedPrefs.getString("mi_user_gender", null);
                String legacyHeight = sharedPrefs.getString("mi_user_height_cm", null);
                String legacyWeigth = sharedPrefs.getString("mi_user_weight_kg", null);
                String legacyYOB = sharedPrefs.getString("mi_user_year_of_birth", null);
                if (legacyGender != null) {
                    int gender = "male".equals(legacyGender) ? 1 : "female".equals(legacyGender) ? 0 : 2;
                    editor.putString(ActivityUser.PREF_USER_GENDER, Integer.toString(gender));
                    editor.remove("mi_user_gender");
                }
                if (legacyHeight != null) {
                    editor.putString(ActivityUser.PREF_USER_HEIGHT_CM, legacyHeight);
                    editor.remove("mi_user_height_cm");
                }
                if (legacyWeigth != null) {
                    editor.putString(ActivityUser.PREF_USER_WEIGHT_KG, legacyWeigth);
                    editor.remove("mi_user_weight_kg");
                }
                if (legacyYOB != null) {
                    editor.putString(ActivityUser.PREF_USER_YEAR_OF_BIRTH, legacyYOB);
                    editor.remove("mi_user_year_of_birth");
                }
                editor.putString(PREFS_VERSION, Integer.toString(CURRENT_PREFS_VERSION));
                break;
            case 1:
                //migrate the integer version of gender introduced in version 1 to a string value, needed for the way Android accesses the shared preferences
                int legacyGender_1 = 2;
                try {
                    legacyGender_1 = sharedPrefs.getInt(ActivityUser.PREF_USER_GENDER, 2);
                } catch (Exception e) {
                    Log.e(TAG, "Could not access legacy activity gender", e);
                }
                editor.putString(ActivityUser.PREF_USER_GENDER, Integer.toString(legacyGender_1));
                //also silently migrate the version to a string value
                editor.putString(PREFS_VERSION, Integer.toString(CURRENT_PREFS_VERSION));
                break;
        }
        editor.apply();
    }

    public static LimitedQueue getIDSenderLookup() {
        return mIDSenderLookup;
    }

    public static boolean isDarkThemeEnabled() {
        return prefs.getString("pref_key_theme", context.getString(R.string.pref_theme_value_light)).equals(context.getString(R.string.pref_theme_value_dark));
    }

    public static int getTextColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.textColor, typedValue, true);
        return typedValue.data;
    }
    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(android.R.attr.background, typedValue, true);
        return typedValue.data;
    }

    public static Prefs getPrefs() {
        return prefs;
    }

    public static GBPrefs getGBPrefs() {
        return gbPrefs;
    }
}
