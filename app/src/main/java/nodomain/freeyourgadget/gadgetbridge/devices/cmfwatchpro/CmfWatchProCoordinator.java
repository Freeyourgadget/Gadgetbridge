/*  Copyright (C) 2024 José Rebelo

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfSpo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.samples.CmfStressSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro.workout.CmfWorkoutSummaryParser;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfHeartRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSleepSessionSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSleepStageSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.CmfStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro.CmfInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro.CmfWatchProSupport;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CmfWatchProCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^Watch Pro$");
    }

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        final ParcelUuid casioService = new ParcelUuid(CmfWatchProSupport.UUID_SERVICE_CMF_CMD);
        final ScanFilter filter = new ScanFilter.Builder().setServiceUuid(casioService).build();
        return Collections.singletonList(filter);
    }

    @Nullable
    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        final CmfInstallHandler handler = new CmfInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean suggestUnbindBeforePair() {
        return false;
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        session.getCmfActivitySampleDao().queryBuilder()
                .where(CmfActivitySampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getCmfStressSampleDao().queryBuilder()
                .where(CmfStressSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getCmfHeartRateSampleDao().queryBuilder()
                .where(CmfHeartRateSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getCmfSleepSessionSampleDao().queryBuilder()
                .where(CmfSleepSessionSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getCmfSleepStageSampleDao().queryBuilder()
                .where(CmfSleepStageSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getCmfSpo2SampleDao().queryBuilder()
                .where(CmfSpo2SampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public String getManufacturer() {
        return "Nothing";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_nothing_cmf_watch_pro;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_amazfit_bip_disabled;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return CmfWatchProSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        final byte[] authKeyBytes = authKey.trim().getBytes();
        return authKeyBytes.length == 32 || (authKey.startsWith("0x") && authKeyBytes.length == 34);
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{R.xml.devicesettings_pairingkey};
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, DaoSession session) {
        return new CmfActivitySampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        return new CmfStressSampleProvider(device, session);
    }

    @Override
    public int[] getStressRanges() {
        // 1-29 = relaxed
        // 30-59 = normal
        // 60-79 = medium
        // 80-99 = high
        return new int[]{1, 30, 60, 80};
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        return new CmfSpo2SampleProvider(device, session);
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new CmfWorkoutSummaryParser(device);
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return 5;
    }

    @Override
    public boolean supportsAlarmTitle(final GBDevice device) {
        return true;
    }

    @Override
    public int getAlarmTitleLimit(final GBDevice device) {
        return 8;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return false; // TODO for watchface management
    }

    @Override
    public boolean supportsCachedAppManagement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsInstalledAppManagement(GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsWatchfaceManagement(GBDevice device) {
        return supportsAppsManagement(device);
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return AppManagerActivity.class;
    }

    @Override
    public boolean supportsAppListFetching() {
        return false; // TODO it does not, but we can fake it for watchfaces
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
    public boolean supportsSpo2(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        return 20;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return false;
    }

    @Override
    public boolean supportsRemSleep() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsFindDevice() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        final List<Integer> settings = new ArrayList<>();

        settings.add(R.xml.devicesettings_header_time);
        settings.add(R.xml.devicesettings_timeformat);

        settings.add(R.xml.devicesettings_header_display);
        settings.add(R.xml.devicesettings_workout_activity_types);
        settings.add(R.xml.devicesettings_liftwrist_display_noshed);

        settings.add(R.xml.devicesettings_header_health);
        settings.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2);
        settings.add(R.xml.devicesettings_inactivity_dnd);
        settings.add(R.xml.devicesettings_hydration_reminder_dnd);

        settings.add(R.xml.devicesettings_header_notifications);
        settings.add(R.xml.devicesettings_send_app_notifications);
        settings.add(R.xml.devicesettings_transliteration);

        settings.add(R.xml.devicesettings_header_other);
        if (getContactsSlotCount(device) > 0) {
            settings.add(R.xml.devicesettings_contacts);
        }

        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new CmfWatchProSettingsCustomizer();
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return null;
        // FIXME language setting does not seem to work from phone
        //return new String[]{
        //        "auto",
        //        "ar_SA",
        //        "de_DE",
        //        "en_US",
        //        "es_ES",
        //        "fr_FR",
        //        "hi_IN",
        //        "id_ID",
        //        "it_IT",
        //        "ja_JP",
        //        "ko_KO",
        //        "zh_CN",
        //        "zh_HK",
        //};
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.SMART
        );
    }

    protected static Prefs getPrefs(final GBDevice device) {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
    }

    public boolean supportsSunriseSunset() {
        return false;
    }
}
