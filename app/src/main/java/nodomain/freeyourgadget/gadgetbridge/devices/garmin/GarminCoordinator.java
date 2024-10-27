package nodomain.freeyourgadget.gadgetbridge.devices.garmin;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.WorkoutVo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.GarminCapability;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminBodyEnergySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminEventSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHrvSummarySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminHrvValueSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.GarminStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.PendingFileDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.RestingMetabolicRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class GarminCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    public boolean suggestUnbindBeforePair() {
        return false;
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice, @NonNull final Device device, @NonNull final DaoSession session) throws GBException {
        deleteAllActivityData(device, session);
    }

    public void deleteAllActivityData(@NonNull final Device device, @NonNull final DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        final Map<AbstractDao<?, ?>, Property> daoMap = new HashMap<AbstractDao<?, ?>, Property>() {{
            put(session.getGarminActivitySampleDao(), GarminActivitySampleDao.Properties.DeviceId);
            put(session.getGarminStressSampleDao(), GarminStressSampleDao.Properties.DeviceId);
            put(session.getGarminBodyEnergySampleDao(), GarminBodyEnergySampleDao.Properties.DeviceId);
            put(session.getGarminSpo2SampleDao(), GarminSpo2SampleDao.Properties.DeviceId);
            put(session.getGarminSleepStageSampleDao(), GarminSleepStageSampleDao.Properties.DeviceId);
            put(session.getGarminEventSampleDao(), GarminEventSampleDao.Properties.DeviceId);
            put(session.getGarminHrvSummarySampleDao(), GarminHrvSummarySampleDao.Properties.DeviceId);
            put(session.getGarminHrvValueSampleDao(), GarminHrvValueSampleDao.Properties.DeviceId);
            put(session.getBaseActivitySummaryDao(), BaseActivitySummaryDao.Properties.DeviceId);
            put(session.getPendingFileDao(), PendingFileDao.Properties.DeviceId);
        }};

        for (final Map.Entry<AbstractDao<?, ?>, Property> e : daoMap.entrySet()) {
            e.getKey().queryBuilder()
                    .where(e.getValue().eq(deviceId))
                    .buildDelete().executeDeleteWithoutDetachingEntities();
        }
    }

    @Override
    public String getManufacturer() {
        return "Garmin";
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_zetime;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_zetime_disabled;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return GarminSupport.class;
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new GarminWorkoutParser(context);
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, DaoSession session) {
        return new GarminActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminStressSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends BodyEnergySample> getBodyEnergySampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminBodyEnergySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvSummarySample> getHrvSummarySampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminHrvSummarySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HrvValueSample> getHrvValueSampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminHrvValueSampleProvider(device, session);
    }

    @Override
    public Vo2MaxSampleProvider<? extends Vo2MaxSample> getVo2MaxSampleProvider(final GBDevice device, final DaoSession session) {
        return new WorkoutVo2MaxSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends RespiratoryRateSample> getRespiratoryRateSampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminRespiratoryRateSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends RestingMetabolicRateSample> getRestingMetabolicRateProvider(final GBDevice device, final DaoSession session) {
        return new GarminRestingMetabolicRateSampleProvider(device, session);
    }

    @Override
    public GarminHeartRateRestingSampleProvider getHeartRateRestingSampleProvider(final GBDevice device, final DaoSession session) {
        return new GarminHeartRateRestingSampleProvider(device, session);
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        if (supports(device, GarminCapability.REALTIME_SETTINGS)) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_garmin_realtime_settings);
        }

        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);

        notifications.add(R.xml.devicesettings_send_app_notifications);

        if (getCannedRepliesSlotCount(device) > 0) {
            notifications.add(R.xml.devicesettings_canned_reply_16);
            notifications.add(R.xml.devicesettings_canned_dismisscall_16);
        }
        if (getContactsSlotCount(device) > 0) {
            notifications.add(R.xml.devicesettings_contacts);
        }

        final List<Integer> location = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.LOCATION);
        location.add(R.xml.devicesettings_workout_send_gps_to_band);
        if (supportsAgpsUpdates(device)) {
            location.add(R.xml.devicesettings_garmin_agps);
        }

        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_high_mtu);

        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_keep_activity_data_on_device);
        developer.add(R.xml.devicesettings_fetch_unknown_files);

        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(GBDevice device) {
        return new GarminSettingsCustomizer();
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
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }

    @Override
    public boolean supportsBodyEnergy() {
        return true;
    }

    @Override
    public boolean supportsHrvMeasurement() {
        return true;
    }

    @Override
    public boolean supportsVO2Max() {
        return true;
    }

    @Override
    public boolean supportsVO2MaxCycling() {
        return true;
    }

    @Override
    public boolean supportsVO2MaxRunning() {
        return true;
    }

    @Override
    public boolean supportsActiveCalories() {
        return true;
    }

    @Override
    public boolean supportsRestingCalories() {
        return true;
    }

    @Override
    public int[] getStressRanges() {
        // 1-25 = relaxed
        // 26-50 = low
        // 51-80 = moderate
        // 76-100 = high
        return new int[]{1, 26, 51, 76};
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsHeartRateRestingMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsRemSleep() {
        return true;
    }

    @Override
    public boolean supportsAwakeSleep() {
        return true;
    }

    @Override
    public boolean supportsRespiratoryRate() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        if (getPrefs(device).getBoolean(GarminPreferences.PREF_FEAT_CANNED_MESSAGES, false)) {
            return 16;
        }

        return 0;
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        if (getPrefs(device).getBoolean(GarminPreferences.PREF_FEAT_CONTACTS, false)) {
            return 50;
        }

        return 0;
    }

    protected static Prefs getPrefs(final GBDevice device) {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    public boolean supportsAgpsUpdates(final GBDevice device) {
        return !getPrefs(device).getString(GarminPreferences.PREF_AGPS_KNOWN_URLS, "").isEmpty();
    }

    public boolean supports(final GBDevice device, final GarminCapability capability) {
        return getPrefs(device).getStringSet(GarminPreferences.PREF_GARMIN_CAPABILITIES, Collections.emptySet())
                .contains(capability.name());
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        final GarminFitFileInstallHandler fitFileInstallHandler = new GarminFitFileInstallHandler(uri, context);
        if (fitFileInstallHandler.isValid())
            return fitFileInstallHandler;

        final GarminGpxRouteInstallHandler garminGpxRouteInstallHandler = new GarminGpxRouteInstallHandler(uri, context);
        if (garminGpxRouteInstallHandler.isValid())
            return garminGpxRouteInstallHandler;

        return null;
    }
}
