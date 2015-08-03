package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public class MiBandCoordinator implements DeviceCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBandCoordinator.class);
    private final MiBandSampleProvider sampleProvider;

    public MiBandCoordinator() {
        sampleProvider = new MiBandSampleProvider();
    }

    @Override
    public boolean supports(GBDeviceCandidate candidate) {
        return candidate.getMacAddress().toUpperCase().startsWith(MiBandService.MAC_ADDRESS_FILTER);
    }

    @Override
    public boolean supports(GBDevice device) {
        return getDeviceType().equals(device.getType());
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND;
    }

    @Override
    public Class<? extends Activity> getPairingActivity() {
        return MiBandPairingActivity.class;
    }

    public Class<? extends Activity> getPrimaryActivity() {
        return ChartsActivity.class;
    }

    @Override
    public SampleProvider getSampleProvider() {
        return sampleProvider;
    }

    public static boolean hasValidUserInfo() {
        String dummyMacAddress = MiBandService.MAC_ADDRESS_FILTER + ":00:00:00";
        try {
            UserInfo userInfo = getConfiguredUserInfo(dummyMacAddress);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Returns the configured user info, or, if that is not available or invalid,
     * a default user info.
     *
     * @param miBandAddress
     */
    public static UserInfo getAnyUserInfo(String miBandAddress) {
        try {
            return getConfiguredUserInfo(miBandAddress);
        } catch (Exception ex) {
            LOG.error("Error creating user info from settings, using default user instead: " + ex);
            return UserInfo.getDefault(miBandAddress);
        }
    }

    /**
     * Returns the user info from the user configured data in the preferences.
     *
     * @param miBandAddress
     * @throws IllegalArgumentException when the user info can not be created
     */
    public static UserInfo getConfiguredUserInfo(String miBandAddress) throws IllegalArgumentException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        int userYear = Integer.parseInt(prefs.getString(MiBandConst.PREF_USER_YEAR_OF_BIRTH, "0"));
        int age = 25;
        if (userYear > 1900) {
            age = Calendar.getInstance().get(Calendar.YEAR) - userYear;
            if (age <= 0) {
                age = 25;
            }
        }
        UserInfo info = UserInfo.create(
                miBandAddress,
                prefs.getString(MiBandConst.PREF_USER_ALIAS, null),
                ("male".equals(prefs.getString(MiBandConst.PREF_USER_GENDER, null)) ? 1 : 0),
                age,
                Integer.parseInt(prefs.getString(MiBandConst.PREF_USER_HEIGHT_CM, "175")),
                Integer.parseInt(prefs.getString(MiBandConst.PREF_USER_WEIGHT_KG, "70")),
                0
        );
        return info;
    }

    public static int getWearLocation(String miBandAddress) throws IllegalArgumentException {
        int location = 0; //left hand
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        if (prefs.getString(MiBandConst.PREF_MIBAND_WEARSIDE, "left") == "right") {
            location = 1; // right hand
        }
        return location;
    }

    public static int getFitnessGoal(String miBandAddress) throws IllegalArgumentException {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        return Integer.parseInt(prefs.getString(MiBandConst.PREF_MIBAND_FITNESS_GOAL, "10000"));
    }
}
