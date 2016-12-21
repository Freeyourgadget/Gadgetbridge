package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.UserInfo;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HPlusCoordinator extends AbstractDeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(HPlusCoordinator.class);
    private static Prefs prefs  = GBApplication.getPrefs();

    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
        LOG.debug("Looking for: " + name);
        if (name != null && name.startsWith("HPLUS")) {
            return DeviceType.HPLUS;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HPLUS;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return null;
    }

    @Override
    public Class<? extends Activity> getPrimaryActivity() {
        return ChartsActivity.class;
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        return null;
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
        return new HPlusSampleProvider(device, session);
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsAlarmConfiguration() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public int getTapString() {
        return R.string.tap_connected_device_for_activity;
    }

    @Override
    public String getManufacturer() {
        return "Zeblaze";
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
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {
        // nothing to delete, yet
    }

    public static int getFitnessGoal(String address) throws IllegalArgumentException {
        Prefs prefs = GBApplication.getPrefs();
        return prefs.getInt(HPlusConstants.PREF_HPLUS_FITNESS_GOAL + "_" + address, 10000);
    }

    /**
     * Returns the user info from the user configured data in the preferences.
     *
     * @param hplusAddress
     * @throws IllegalArgumentException when the user info can not be created
     */
    public static UserInfo getConfiguredUserInfo(String hplusAddress) throws IllegalArgumentException {
        ActivityUser activityUser = new ActivityUser();

        UserInfo info = UserInfo.create(
                hplusAddress,
                prefs.getString(HPlusConstants.PREF_HPLUS_USER_ALIAS, null),
                activityUser.getGender(),
                activityUser.getAge(),
                activityUser.getHeightCm(),
                activityUser.getWeightKg(),
                0
        );
        return info;
    }

    public static byte getCountry(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_COUNTRY + "_" + address, 10);

    }

    public static byte getTimeMode(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_TIMEMODE + "_" + address, 0);
    }

    public static byte getUnit(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_UNIT + "_" + address, 0);
    }

    public static byte getUserWeight(String address) {
        ActivityUser activityUser = new ActivityUser();

        return (byte) activityUser.getWeightKg();
    }

    public static byte getUserHeight(String address) {
        ActivityUser activityUser = new ActivityUser();

        return (byte) activityUser.getHeightCm();
    }

    public static byte getUserAge(String address) {
        ActivityUser activityUser = new ActivityUser();

        return (byte) activityUser.getAge();
    }

    public static byte getUserSex(String address) {
        ActivityUser activityUser = new ActivityUser();

        int gender = activityUser.getGender();

        return (byte) gender;

    }

    public static int getGoal(String address) {
        ActivityUser activityUser = new ActivityUser();

        return activityUser.getStepsGoal();
    }

    public static byte getScreenTime(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_SCREENTIME + "_" + address, 5);

    }

    public static byte getAllDayHR(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_ALLDAYHR + "_" + address, 10);

    }

    public static byte getSocial(String address) {

        //TODO: Figure what this is. Returning the default value

        return (byte) 255;
    }

    public static byte getUserWrist(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_WRIST + "_" + address, 10);

    }

    public static boolean getSWAlertTime(String address) {
        return (boolean) prefs.getBoolean(HPlusConstants.PREF_HPLUS_SWALERT + "_" + address, false);


    }

    public static int getAlertTime(String address) {
        return (int) prefs.getInt(HPlusConstants.PREF_HPLUS_ALERT_TIME + "_" + address, 0);

    }

    public static int getSITStartTime(String address) {
        return (int) prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_START_TIME + "_" + address, 0);

    }

    public static int getSITEndTime(String address) {
        return (int) prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_END_TIME + "_" + address, 0);

    }

}
