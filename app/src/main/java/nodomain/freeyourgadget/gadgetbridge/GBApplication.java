/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Martin, Matthieu Baerts, Normano64, Pavel Elagin, Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge;

import android.annotation.TargetApi;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.NotificationManager.Policy;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.util.TypedValue;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBOpenHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceManager;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.externalevents.BluetoothStateChangeReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.NotificationCollectorMonitorService;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.NOTIFICATION_CHANNEL_ID;

/**
 * Main Application class that initializes and provides access to certain things like
 * logging and DB access.
 */
public class GBApplication extends Application {
    // Since this class must not log to slf4j, we use plain android.util.Log
    private static final String TAG = "GBApplication";
    public static final String DATABASE_NAME = "Gadgetbridge";

    private static GBApplication context;
    private static final Lock dbLock = new ReentrantLock();
    private static DeviceService deviceService;
    private static SharedPreferences sharedPrefs;
    private static final String PREFS_VERSION = "shared_preferences_version";
    //if preferences have to be migrated, increment the following and add the migration logic in migratePrefs below; see http://stackoverflow.com/questions/16397848/how-can-i-migrate-android-preferences-with-a-new-version
    private static final int CURRENT_PREFS_VERSION = 2;
    private static LimitedQueue mIDSenderLookup = new LimitedQueue(16);
    private static Prefs prefs;
    private static GBPrefs gbPrefs;
    private static LockHandler lockHandler;
    /**
     * Note: is null on Lollipop and Kitkat
     */
    private static NotificationManager notificationManager;

    public static final String ACTION_QUIT
            = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.quit";
    public static final String ACTION_LANGUAGE_CHANGE = "nodomain.freeyourgadget.gadgetbridge.gbapplication.action.language_change";

    private static GBApplication app;

    private static Logging logging = new Logging() {
        @Override
        protected String createLogDirectory() throws IOException {
            if (GBEnvironment.env().isLocalTest()) {
                return System.getProperty(Logging.PROP_LOGFILES_DIR);
            } else {
                File dir = FileUtils.getExternalFilesDir();
                return dir.getAbsolutePath();
            }
        }
    };
    private static Locale language;

    private DeviceManager deviceManager;
    private BluetoothStateChangeReceiver bluetoothStateChangeReceiver;

    public static void quit() {
        GB.log("Quitting Gadgetbridge...", GB.INFO, null);
        Intent quitIntent = new Intent(GBApplication.ACTION_QUIT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(quitIntent);
        GBApplication.deviceService().quit();
    }

    public GBApplication() {
        context = this;
        // don't do anything here, add it to onCreate instead
    }

    public static Logging getLogging() {
        return logging;
    }

    protected DeviceService createDeviceService() {
        return new GBDeviceService(this);
    }

    @Override
    public void onCreate() {
        app = this;
        super.onCreate();

        if (lockHandler != null) {
            // guard against multiple invocations (robolectric)
            return;
        }

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs = new Prefs(sharedPrefs);
        gbPrefs = new GBPrefs(prefs);

        if (!GBEnvironment.isEnvironmentSetup()) {
            GBEnvironment.setupEnvironment(GBEnvironment.createDeviceEnvironment());
            // setup db after the environment is set up, but don't do it in test mode
            // in test mode, it's done individually, see TestBase
            setupDatabase();
        }

        // don't do anything here before we set up logging, otherwise
        // slf4j may be implicitly initialized before we properly configured it.
        setupLogging(isFileLoggingEnabled());

        if (getPrefsFileVersion() != CURRENT_PREFS_VERSION) {
            migratePrefs(getPrefsFileVersion());
        }

        setupExceptionHandler();

        deviceManager = new DeviceManager(this);
        String language = prefs.getString("language", "default");
        setLanguage(language);

        deviceService = createDeviceService();
        loadAppsNotifBlackList();
        loadAppsPebbleBlackList();
        loadCalendarsBlackList();

        if (isRunningMarshmallowOrLater()) {
            notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            //the following will ensure the notification manager is kept alive
            if(isRunningOreoOrLater()) {
                NotificationChannel channel = notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID);
                if(channel == null) {
                    channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                            getString(R.string.notification_channel_name),
                            NotificationManager.IMPORTANCE_LOW);
                    notificationManager.createNotificationChannel(channel);
                }

                bluetoothStateChangeReceiver = new BluetoothStateChangeReceiver();
                registerReceiver(bluetoothStateChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
            }
            startService(new Intent(this, NotificationCollectorMonitorService.class));
        }
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level >= TRIM_MEMORY_BACKGROUND) {
            if (!hasBusyDevice()) {
                DBHelper.clearSession();
            }
        }
    }

    /**
     * Returns true if at least a single device is busy, e.g synchronizing activity data
     * or something similar.
     * Note: busy is not the same as connected or initialized!
     */
    private boolean hasBusyDevice() {
        List<GBDevice> devices = getDeviceManager().getDevices();
        for (GBDevice device : devices) {
            if (device.isBusy()) {
                return true;
            }
        }
        return false;
    }

    public static void setupLogging(boolean enabled) {
        logging.setupLogging(enabled);
    }

    public static String getLogPath(){
        return logging.getLogPath();
    }

    private void setupExceptionHandler() {
        LoggingExceptionHandler handler = new LoggingExceptionHandler(Thread.getDefaultUncaughtExceptionHandler());
        Thread.setDefaultUncaughtExceptionHandler(handler);
    }

    public static boolean isFileLoggingEnabled() {
        return prefs.getBoolean("log_to_file", false);
    }

    public static boolean minimizeNotification() {
        return prefs.getBoolean("minimize_priority", false);
    }

    public void setupDatabase() {
        DaoMaster.OpenHelper helper;
        GBEnvironment env = GBEnvironment.env();
        if (env.isTest()) {
            helper = new DaoMaster.DevOpenHelper(this, null, null);
        } else {
            helper = new DBOpenHelper(this, DATABASE_NAME, null);
        }
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        if (lockHandler == null) {
            lockHandler = new LockHandler();
        }
        lockHandler.init(daoMaster, helper);
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
     * <p>
     * Callers must not hold a reference to the returned instance because it
     * will be invalidated at some point.
     *
     * @return the DBHandler
     * @throws GBException
     * @see #releaseDB()
     */
    public static DBHandler acquireDB() throws GBException {
        try {
            if (dbLock.tryLock(30, TimeUnit.SECONDS)) {
                return lockHandler;
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
    public static boolean isRunningNougatOrLater() {
        return VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    public static boolean isRunningOreoOrLater(){
        return VERSION.SDK_INT >= Build.VERSION_CODES.O;
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
        if (priorityType == Policy.PRIORITY_CATEGORY_MESSAGES) {
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

    private static HashSet<String> apps_notification_blacklist = null;

    public static boolean appIsNotifBlacklisted(String packageName) {
        if (apps_notification_blacklist == null) {
            GB.log("appIsNotifBlacklisted: apps_notification_blacklist is null!", GB.INFO, null);
        }
        return apps_notification_blacklist != null && apps_notification_blacklist.contains(packageName);
    }

    public static void setAppsNotifBlackList(Set<String> packageNames) {
        if (packageNames == null) {
            GB.log("Set null apps_notification_blacklist", GB.INFO, null);
            apps_notification_blacklist = new HashSet<>();
        } else {
            apps_notification_blacklist = new HashSet<>(packageNames);
        }
        GB.log("New apps_notification_blacklist has " + apps_notification_blacklist.size() + " entries", GB.INFO, null);
        saveAppsNotifBlackList();
    }

    private static void loadAppsNotifBlackList() {
        GB.log("Loading apps_notification_blacklist", GB.INFO, null);
        apps_notification_blacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.PACKAGE_BLACKLIST, null); // lgtm [java/abstract-to-concrete-cast]
        if (apps_notification_blacklist == null) {
            apps_notification_blacklist = new HashSet<>();
        }
        GB.log("Loaded apps_notification_blacklist has " + apps_notification_blacklist.size() + " entries", GB.INFO, null);
    }

    private static void saveAppsNotifBlackList() {
        GB.log("Saving apps_notification_blacklist with " + apps_notification_blacklist.size() + " entries", GB.INFO, null);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (apps_notification_blacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.PACKAGE_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.PACKAGE_BLACKLIST, apps_notification_blacklist);
        }
        editor.apply();
    }

    public static void addAppToNotifBlacklist(String packageName) {
        if (apps_notification_blacklist.add(packageName)) {
            saveAppsNotifBlackList();
        }
    }

    public static synchronized void removeFromAppsNotifBlacklist(String packageName) {
        GB.log("Removing from apps_notification_blacklist: " + packageName, GB.INFO, null);
        apps_notification_blacklist.remove(packageName);
        saveAppsNotifBlackList();
    }

    private static HashSet<String> apps_pebblemsg_blacklist = null;

    public static boolean appIsPebbleBlacklisted(String sender) {
        if (apps_pebblemsg_blacklist == null) {
            GB.log("appIsPebbleBlacklisted: apps_pebblemsg_blacklist is null!", GB.INFO, null);
        }
        return apps_pebblemsg_blacklist != null && apps_pebblemsg_blacklist.contains(sender);
    }

    public static void setAppsPebbleBlackList(Set<String> packageNames) {
        if (packageNames == null) {
            GB.log("Set null apps_pebblemsg_blacklist", GB.INFO, null);
            apps_pebblemsg_blacklist = new HashSet<>();
        } else {
            apps_pebblemsg_blacklist = new HashSet<>(packageNames);
        }
        GB.log("New apps_pebblemsg_blacklist has " + apps_pebblemsg_blacklist.size() + " entries", GB.INFO, null);
        saveAppsPebbleBlackList();
    }

    private static void loadAppsPebbleBlackList() {
        GB.log("Loading apps_pebblemsg_blacklist", GB.INFO, null);
        apps_pebblemsg_blacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST, null); // lgtm [java/abstract-to-concrete-cast]
        if (apps_pebblemsg_blacklist == null) {
            apps_pebblemsg_blacklist = new HashSet<>();
        }
        GB.log("Loaded apps_pebblemsg_blacklist has " + apps_pebblemsg_blacklist.size() + " entries", GB.INFO, null);
    }

    private static void saveAppsPebbleBlackList() {
        GB.log("Saving apps_pebblemsg_blacklist with " + apps_pebblemsg_blacklist.size() + " entries", GB.INFO, null);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (apps_pebblemsg_blacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.PACKAGE_PEBBLEMSG_BLACKLIST, apps_pebblemsg_blacklist);
        }
        editor.apply();
    }

    public static void addAppToPebbleBlacklist(String packageName) {
        if (apps_pebblemsg_blacklist.add(packageNameToPebbleMsgSender(packageName))) {
            saveAppsPebbleBlackList();
        }
    }

    public static synchronized void removeFromAppsPebbleBlacklist(String packageName) {
        GB.log("Removing from apps_pebblemsg_blacklist: " + packageName, GB.INFO, null);
        apps_pebblemsg_blacklist.remove(packageNameToPebbleMsgSender(packageName));
        saveAppsPebbleBlackList();
    }

public static String packageNameToPebbleMsgSender(String packageName) {
    if ("eu.siacs.conversations".equals(packageName)){
        return("Conversations");
    } else if ("net.osmand.plus".equals(packageName)) {
        return("OsmAnd");
    }
    return packageName;
}

    private static HashSet<String> calendars_blacklist = null;

    public static boolean calendarIsBlacklisted(String calendarDisplayName) {
        if (calendars_blacklist == null) {
            GB.log("calendarIsBlacklisted: calendars_blacklist is null!", GB.INFO, null);
        }
        return calendars_blacklist != null && calendars_blacklist.contains(calendarDisplayName);
    }

    public static void setCalendarsBlackList(Set<String> calendarNames) {
        if (calendarNames == null) {
            GB.log("Set null apps_notification_blacklist", GB.INFO, null);
            calendars_blacklist = new HashSet<>();
        } else {
            calendars_blacklist = new HashSet<>(calendarNames);
        }
        GB.log("New calendars_blacklist has " + calendars_blacklist.size() + " entries", GB.INFO, null);
        saveCalendarsBlackList();
    }

    public static void addCalendarToBlacklist(String calendarDisplayName) {
        if (calendars_blacklist.add(calendarDisplayName)) {
            saveCalendarsBlackList();
        }
    }

    public static void removeFromCalendarBlacklist(String calendarDisplayName) {
        calendars_blacklist.remove(calendarDisplayName);
        saveCalendarsBlackList();
    }

    private static void loadCalendarsBlackList() {
        GB.log("Loading calendars_blacklist", GB.INFO, null);
        calendars_blacklist = (HashSet<String>) sharedPrefs.getStringSet(GBPrefs.CALENDAR_BLACKLIST, null); // lgtm [java/abstract-to-concrete-cast]
        if (calendars_blacklist == null) {
            calendars_blacklist = new HashSet<>();
        }
        GB.log("Loaded calendars_blacklist has " + calendars_blacklist.size() + " entries", GB.INFO, null);
    }

    private static void saveCalendarsBlackList() {
        GB.log("Saving calendars_blacklist with " + calendars_blacklist.size() + " entries", GB.INFO, null);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        if (calendars_blacklist.isEmpty()) {
            editor.putStringSet(GBPrefs.CALENDAR_BLACKLIST, null);
        } else {
            Prefs.putStringSet(editor, GBPrefs.CALENDAR_BLACKLIST, calendars_blacklist);
        }
        editor.apply();
    }

    /**
     * Deletes both the old Activity database and the new one recreates it with empty tables.
     *
     * @return true on successful deletion
     */
    public static synchronized boolean deleteActivityDatabase(Context context) {
        // TODO: flush, close, reopen db
        if (lockHandler != null) {
            lockHandler.closeDb();
        }
        boolean result = deleteOldActivityDatabase(context);
        result &= getContext().deleteDatabase(DATABASE_NAME);
        return result;
    }

    /**
     * Deletes the legacy (pre 0.12) Activity database
     *
     * @return true on successful deletion
     */
    public static synchronized boolean deleteOldActivityDatabase(Context context) {
        DBHelper dbHelper = new DBHelper(context);
        boolean result = true;
        if (dbHelper.existsDB("ActivityDatabase")) {
            result = getContext().deleteDatabase("ActivityDatabase");
        }
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
                String legacyWeight = sharedPrefs.getString("mi_user_weight_kg", null);
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
                if (legacyWeight != null) {
                    editor.putString(ActivityUser.PREF_USER_WEIGHT_KG, legacyWeight);
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

    public static void setLanguage(String lang) {
        if (lang.equals("default")) {
            language = Resources.getSystem().getConfiguration().locale;
        } else {
            language = new Locale(lang);
        }
        updateLanguage(language);
    }

    public static void updateLanguage(Locale locale) {
        AndroidUtils.setLanguage(context, locale);

        Intent intent = new Intent();
        intent.setAction(ACTION_LANGUAGE_CHANGE);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
        theme.resolveAttribute(R.attr.textColorPrimary, typedValue, true);
        return typedValue.data;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLanguage(getLanguage());
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

    public DeviceManager getDeviceManager() {
        return deviceManager;
    }

    public static GBApplication app() {
        return app;
    }

    public static Locale getLanguage() {
        return language;
    }

    public String getVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            GB.log("Unable to determine Gadgetbridge's version", GB.WARN, e);
            return "0.0.0";
        }
    }

    public String getNameAndVersion() {
        try {
            ApplicationInfo appInfo = getPackageManager().getApplicationInfo(getContext().getPackageName(), PackageManager.GET_META_DATA);
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            return String.format("%s %s", appInfo.name, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            GB.log("Unable to determine Gadgetbridge's name/version", GB.WARN, e);
            return "Gadgetbridge";
        }
    }
}
