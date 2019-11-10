package nodomain.freeyourgadget.gadgetbridge.devices.lenovo.watchxplus;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.lenovo.LenovoWatchPairingActivity;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lenovo.watchxplus.WatchXPlusDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.GBApplication.getContext;


public class WatchXPlusDeviceCoordinator extends AbstractDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(WatchXPlusDeviceSupport.class);
    public static final int FindPhone_ON = -1;
    public static final int FindPhone_OFF = 0;
    public static boolean isBPCalibrated = false;

    protected static Prefs prefs  = GBApplication.getPrefs();

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid watchXpService = new ParcelUuid(WatchXPlusConstants.UUID_SERVICE_WATCHXPLUS);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(watchXpService).build();
        return Collections.singletonList(filter);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String macAddress = candidate.getMacAddress().toUpperCase();
        String deviceName = candidate.getName().toUpperCase();
        if (candidate.supportsService(WatchXPlusConstants.UUID_SERVICE_WATCHXPLUS)) {
            return DeviceType.WATCHXPLUS;
        } else if (macAddress.startsWith("DC:41:E5")) {
            return DeviceType.WATCHXPLUS;
        } else if (deviceName.equalsIgnoreCase("WATCH XPLUS")) {
            return DeviceType.WATCHXPLUS;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.WATCHXPLUS;
    }

    @Nullable
    @Override
    public Class<? extends Activity> getPairingActivity() {
        return LenovoWatchPairingActivity.class;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new WatchXPlusSampleProvider(device, session);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public int getAlarmSlotCount() {
        return 3;
    }

    @Override
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Lenovo";
    }

    @Override
    public boolean supportsAppsManagement() {
        return false;
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return null;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return false;
    }

    @Override
    public boolean supportsRealtimeData() { return false; }
    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() { return false; }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_disconnectnotification,
                R.xml.devicesettings_find_phone,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_donotdisturb_no_auto
        };
    }

/*
Prefs from device settings on main page
 */
// return saved time format
    public static byte getTimeMode(SharedPreferences sharedPrefs) {
        String timeMode = sharedPrefs.getString(DeviceSettingsPreferenceConst.PREF_TIMEFORMAT, getContext().getString(R.string.p_timeformat_24h));
        if (timeMode.equals(getContext().getString(R.string.p_timeformat_24h))) {
            return WatchXPlusConstants.ARG_SET_TIMEMODE_24H;
        } else {
            return WatchXPlusConstants.ARG_SET_TIMEMODE_12H;
        }
    }

// check if it is needed to toggle Lift Wrist to Sreen on
    public static boolean shouldEnableHeadsUpScreen(SharedPreferences sharedPrefs) {
        String liftMode = sharedPrefs.getString(WatchXPlusConstants.PREF_ACTIVATE_DISPLAY, getContext().getString(R.string.p_on));
        // WatchXPlus doesn't support scheduled intervals. Treat it as "on".
        return !liftMode.equals(getContext().getString(R.string.p_off));
    }

// check if it is needed to toggle Disconnect reminder
    public static boolean shouldEnableDisconnectReminder(SharedPreferences sharedPrefs) {
        String lostReminder = sharedPrefs.getString(WatchXPlusConstants.PREF_DISCONNECT_REMIND, getContext().getString(R.string.p_on));
        // WatchXPlus doesn't support scheduled intervals. Treat it as "on".
        return !lostReminder.equals(getContext().getString(R.string.p_off));
    }

// find phone settings
    /**
     * @return {@link #FindPhone_OFF}, {@link #FindPhone_ON}, or the duration
     */
    public static int getFindPhone(SharedPreferences sharedPrefs) {
        String findPhone = sharedPrefs.getString(WatchXPlusConstants.PREF_FIND_PHONE, getContext().getString(R.string.p_off));

        if (findPhone.equals(getContext().getString(R.string.p_off))) {
            return FindPhone_OFF;
        } else if (findPhone.equals(getContext().getString(R.string.p_on))) {
            return FindPhone_ON;
        } else { // Duration
            String duration = sharedPrefs.getString(WatchXPlusConstants.PREF_FIND_PHONE_DURATION, "0");

            try {
                int iDuration;

                try {
                    iDuration = Integer.valueOf(duration);
                } catch (Exception ex) {
                    iDuration = 60;
                }

                return iDuration;
            } catch (Exception e) {
                return FindPhone_ON;
            }
        }
    }

    /**
     * @param startOut out Only hour/minute are used.
     * @param endOut   out Only hour/minute are used.
     * @return True if quite hours are enabled.
     */
    public static boolean getQuiteHours(SharedPreferences sharedPrefs, Calendar startOut, Calendar endOut) {
        String doNotDisturb = sharedPrefs.getString(WatchXPlusConstants.PREF_DO_NOT_DISTURB, getContext().getString(R.string.p_off));

        if (doNotDisturb.equals(getContext().getString(R.string.p_off))) {
            LOG.info(" DND is disabled ");
            return false;
        } else {
            String start = sharedPrefs.getString(WatchXPlusConstants.PREF_DO_NOT_DISTURB_START, "00:00");
            String end = sharedPrefs.getString(WatchXPlusConstants.PREF_DO_NOT_DISTURB_END, "00:00");

            DateFormat df = new SimpleDateFormat("HH:mm");

            try {
                startOut.setTime(df.parse(start));
                endOut.setTime(df.parse(end));

                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }

/*
Values from device specific settings page
 */
// read altitude from preferences
    public static int getAltitude(String address) {
        return (int) prefs.getInt(WatchXPlusConstants.PREF_ALTITUDE, 200);
    }

// read repeat call notification
    public static int getRepeatOnCall(String address) {
        return (int) prefs.getInt(WatchXPlusConstants.PREF_REPEAT, 1);
    }

//read continious call notification
    public static boolean getContiniousVibrationOnCall(String address) {
        return (boolean) prefs.getBoolean(WatchXPlusConstants.PREF_CONTINIOUS, false);
    }

//read missed call notification
    public static boolean getMissedCallReminder(String address) {
        return (boolean) prefs.getBoolean(WatchXPlusConstants.PREF_MISSED_CALL, false);
    }

//read button reject call settings
    public static boolean getButtonReject(String address) {
        return (boolean) prefs.getBoolean(WatchXPlusConstants.PREF_BUTTON_REJECT, false);
    }

//read shake wrist reject call settings
    public static boolean getShakeReject(String address) {
        return (boolean) prefs.getBoolean(WatchXPlusConstants.PREF_SHAKE_REJECT, false);
    }

/*
Other saved preferences
 */



}
