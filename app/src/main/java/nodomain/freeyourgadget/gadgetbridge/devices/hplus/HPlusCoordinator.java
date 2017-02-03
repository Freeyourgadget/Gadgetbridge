package nodomain.freeyourgadget.gadgetbridge.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.ChartsActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HPlusHealthActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

public class HPlusCoordinator extends AbstractDeviceCoordinator {
    protected static final Logger LOG = LoggerFactory.getLogger(HPlusCoordinator.class);
    protected static Prefs prefs  = GBApplication.getPrefs();

    @NonNull
    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        ParcelUuid hpService = new ParcelUuid(HPlusConstants.UUID_SERVICE_HP);
        ScanFilter filter = new ScanFilter.Builder().setServiceUuid(hpService).build();
        return Collections.singletonList(filter);
    }

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        String name = candidate.getDevice().getName();
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
        return new HPlusHealthSampleProvider(device, session);
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
    public boolean supportsSmartWakeup(GBDevice device) {
        return false;
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
        Long deviceId = device.getId();
        QueryBuilder<?> qb = session.getHPlusHealthActivitySampleDao().queryBuilder();
        qb.where(HPlusHealthActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
    }

    public static byte getLanguage(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_LANGUAGE + "_" + address, HPlusConstants.ARG_LANGUAGE_EN);

    }

    public static byte getTimeMode(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_TIMEMODE + "_" + address, HPlusConstants.ARG_TIMEMODE_24H);
    }

    public static byte getUnit(String address) {
        return (byte) prefs.getInt(HPlusConstants.PREF_HPLUS_UNIT + "_" + address, 0);
    }

    public static byte getUserWeight(String address) {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getWeightKg() & 0xFF);
    }

    public static byte getUserHeight(String address) {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getHeightCm() & 0xFF);
    }

    public static byte getUserAge(String address) {
        ActivityUser activityUser = new ActivityUser();

        return (byte) (activityUser.getAge() & 0xFF);
    }

    public static byte getUserGender(String address) {
        ActivityUser activityUser = new ActivityUser();

        if (activityUser.getGender() == ActivityUser.GENDER_MALE)
            return HPlusConstants.ARG_GENDER_MALE;

        return HPlusConstants.ARG_GENDER_FEMALE;
    }

    public static int getGoal(String address) {
        ActivityUser activityUser = new ActivityUser();

        return activityUser.getStepsGoal();
    }

    public static byte getScreenTime(String address) {
        return (byte) (prefs.getInt(HPlusConstants.PREF_HPLUS_SCREENTIME + "_" + address, 5) & 0xFF);
    }

    public static byte getAllDayHR(String address) {
        return (byte) (prefs.getInt(HPlusConstants.PREF_HPLUS_ALLDAYHR + "_" + address, HPlusConstants.ARG_HEARTRATE_ALLDAY_ON) & 0xFF);
    }

    public static byte getHRState(String address) {
        return (byte) (prefs.getInt(HPlusConstants.PREF_HPLUS_HR + "_" + address, HPlusConstants.ARG_HEARTRATE_MEASURE_ON) & 0xFF);
    }

    public static byte getSocial(String address) {
        //TODO: Figure what this is. Returning the default value

        return (byte) 255;
    }

    public static byte getUserWrist(String address) {
        return (byte) (prefs.getInt(HPlusConstants.PREF_HPLUS_WRIST + "_" + address, 10) & 0xFF);
    }

    public static int getSITStartTime(String address) {
        return prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_START_TIME + "_" + address, 0);
    }

    public static int getSITEndTime(String address) {
        return prefs.getInt(HPlusConstants.PREF_HPLUS_SIT_END_TIME + "_" + address, 0);
    }

}
