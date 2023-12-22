/*  Copyright (C) 2023 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences.*;

import android.app.Activity;
import android.bluetooth.le.ScanFilter;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.capabilities.widgets.WidgetManager;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepRespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiBleUuids;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiPreferences;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.activity.impl.WorkoutSummaryParser;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class XiaomiCoordinator extends AbstractBLEDeviceCoordinator {
    // On plaintext devices, user id is used as auth key - numeric
    private static final Pattern AUTH_KEY_PATTERN = Pattern.compile("^[0-9]+$");

    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        final List<ScanFilter> filters = new ArrayList<>();
        for (final UUID uuid : XiaomiBleUuids.UUIDS.keySet()) {
            final ParcelUuid service = new ParcelUuid(uuid);
            final ScanFilter filter = new ScanFilter.Builder().setServiceUuid(service).build();
            filters.add(filter);
        }
        return filters;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return XiaomiSupport.class;
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        final byte[] authKeyBytes = authKey.trim().getBytes();
        // At this point we don't know if it's encrypted or not, so let's accept both:
        return authKeyBytes.length == 32 || (authKey.startsWith("0x") && authKeyBytes.length == 34)
                || AUTH_KEY_PATTERN.matcher(authKey.trim()).matches();
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        session.getXiaomiActivitySampleDao().queryBuilder()
                .where(XiaomiActivitySampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, DaoSession session) {
        return new XiaomiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiStressSampleProvider(device, session);
    }

    @Override
    public int[] getStressRanges() {
        // 1-25 = relaxed
        // 26-50 = mild
        // 51-80 = moderate
        // 81-100 = high
        return new int[]{1, 26, 51, 81};
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiSpo2SampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateMaxSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiHeartRateMaxSampleProvider
        return super.getHeartRateMaxSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateRestingSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiHeartRateRestingSampleProvider
        return super.getHeartRateRestingSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends HeartRateSample> getHeartRateManualSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiHeartRateManualSampleProvider
        return super.getHeartRateManualSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends PaiSample> getPaiSampleProvider(final GBDevice device, final DaoSession session) {
        return new XiaomiPaiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends SleepRespiratoryRateSample> getSleepRespiratoryRateSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiSleepRespiratoryRateSampleProvider
        return super.getSleepRespiratoryRateSampleProvider(device, session);
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device) {
        return new WorkoutSummaryParser();
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(XiaomiPreferences.PREF_ALARM_SLOTS, 0);
    }

    @Override
    public boolean supportsSmartWakeup(final GBDevice device) {
        return true;
    }

    public boolean supportsAppsManagement(final GBDevice device) {
        return true;
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
        return true;
    }

    @Override
    public boolean supportsAppReordering() {
        return false;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        // TODO It does, but not yet fully working - only in Mi Band 8
        return GBApplication.isDebug() || GBApplication.isNightly();
    }

    @Override
    public boolean supportsActivityTracking() {
        // TODO It does, but not yet fully working - only in Mi Band 8
        return GBApplication.isDebug() || GBApplication.isNightly();
    }

    @Override
    public boolean supportsActivityTracks() {
        // TODO It does, but not yet fully working - only in Mi Band 8
        return GBApplication.isDebug() || GBApplication.isNightly();
    }

    @Override
    public boolean supportsStressMeasurement() {
        return true;
    }

    @Override
    public boolean supportsSpo2() {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats() {
        // TODO it does, and they're persisted - see DailySummaryParser
        return false;
    }

    @Override
    public boolean supportsPai() {
        // Vitality Score
        return true;
    }

    @Override
    public int getPaiName() {
        return R.string.pref_vitality_score_title;
    }

    @Override
    public boolean supportsPaiTime() {
        return false;
    }

    @Override
    public boolean supportsSleepRespiratoryRate() {
        // TODO it does
        return false;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getMaximumReminderMessageLength() {
        // TODO does it?
        return 20;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(XiaomiPreferences.PREF_REMINDER_SLOTS, 0);
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(XiaomiPreferences.PREF_CANNED_MESSAGES_MAX, 0);
    }

    @Override
    public int getWorldClocksSlotCount() {
        // TODO how many? also, map world clocks
        return 0;
    }

    @Override
    public int getWorldClocksLabelLength() {
        // TODO no labels
        // TODO list of supported timezones
        return 5;
    }

    @Override
    public boolean supportsDisabledWorldClocks() {
        // TODO does it?
        return false;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Xiaomi";
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
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
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        final List<Integer> settings = new ArrayList<>();

        if (supports(device, FEAT_WEAR_MODE)) {
            // TODO we should be able to get this from the band - right now it must be changed
            // at least once from the band itself
            settings.add(R.xml.devicesettings_wearmode);
        }

        //
        // Time
        //
        settings.add(R.xml.devicesettings_header_time);
        settings.add(R.xml.devicesettings_timeformat);
        if (getWorldClocksSlotCount() > 0) {
            settings.add(R.xml.devicesettings_world_clocks);
        }

        //
        // Display
        //
        settings.add(R.xml.devicesettings_header_display);
        if (supports(device, FEAT_DISPLAY_ITEMS)) {
            settings.add(R.xml.devicesettings_xiaomi_displayitems);
        }
        if (this.supportsWidgets(device)) {
            settings.add(R.xml.devicesettings_widgets);
        }
        if (supports(device, FEAT_PASSWORD)) {
            settings.add(R.xml.devicesettings_password);
        }

        //
        // Health
        //
        settings.add(R.xml.devicesettings_header_health);
        if (supportsStressMeasurement() && supports(device, FEAT_STRESS) && supportsSpo2() && supports(device, FEAT_SPO2)) {
            settings.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2);
        } else if (supportsStressMeasurement() && supports(device, FEAT_STRESS)) {
            settings.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress);
        } else {
            settings.add(R.xml.devicesettings_heartrate_sleep_activity);
        }
        if (supports(device, FEAT_INACTIVITY)) {
            settings.add(R.xml.devicesettings_inactivity_dnd_no_threshold);
        }
        if (supports(device, FEAT_SLEEP_MODE_SCHEDULE)) {
            settings.add(R.xml.devicesettings_sleep_mode_schedule);
        }
        if (supports(device, FEAT_GOAL_NOTIFICATION)) {
            settings.add(R.xml.devicesettings_goal_notification);
        }
        if (supports(device, FEAT_GOAL_SECONDARY)) {
            settings.add(R.xml.devicesettings_goal_secondary);
        }
        if (supports(device, FEAT_VITALITY_SCORE)) {
            settings.add(R.xml.devicesettings_vitality_score);
        }

        //
        // Workout
        //
        settings.add(R.xml.devicesettings_header_workout);
        settings.add(R.xml.devicesettings_workout_start_on_phone);
        settings.add(R.xml.devicesettings_workout_send_gps_to_band);

        //
        // Notifications
        //
        settings.add(R.xml.devicesettings_header_notifications);
        // TODO not implemented settings.add(R.xml.devicesettings_vibrationpatterns);
        // TODO not implemented settings.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
        settings.add(R.xml.devicesettings_send_app_notifications);
        if (supports(device, FEAT_SCREEN_ON_ON_NOTIFICATIONS)) {
            settings.add(R.xml.devicesettings_screen_on_on_notifications);
        }
        settings.add(R.xml.devicesettings_autoremove_notifications);
        if (getCannedRepliesSlotCount(device) > 0) {
            settings.add(R.xml.devicesettings_canned_dismisscall_16);
        }

        //
        // Calendar
        //
        if (supportsCalendarEvents()) {
            settings.add(R.xml.devicesettings_header_calendar);
            settings.add(R.xml.devicesettings_sync_calendar);
        }

        //
        // Other
        //
        settings.add(R.xml.devicesettings_header_other);
        if (getContactsSlotCount(device) > 0) {
            settings.add(R.xml.devicesettings_contacts);
        }
        if (supports(device, FEAT_CAMERA_REMOTE)) {
            settings.add(R.xml.devicesettings_camera_remote);
        }
        if (supports(device, FEAT_DEVICE_ACTIONS)) {
            settings.add(R.xml.devicesettings_device_actions);
        }
        settings.add(R.xml.devicesettings_phone_silent_mode);

        //
        // Developer
        //
        settings.add(R.xml.devicesettings_header_developer);
        settings.add(R.xml.devicesettings_keep_activity_data_on_device);

        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{R.xml.devicesettings_pairingkey};
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new XiaomiSettingsCustomizer();
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "ar_SA",
                "cs_CZ",
                "da_DK",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "he_IL",
                "id_ID",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "nl_NL",
                "nb_NO",
                "pl_PL",
                "pt_BR",
                "pt_PT",
                "ro_RO",
                "ru_RU",
                "sv_SE",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CN",
                "zh_TW",
        };
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return PasswordCapabilityImpl.Mode.NUMBERS_6;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.SMART,
                HeartRateCapability.MeasurementInterval.MINUTES_1,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_30
        );
    }

    @Override
    public boolean supportsWidgets(final GBDevice device) {
        return getPrefs(device).getBoolean(XiaomiPreferences.FEAT_WIDGETS, false);
    }

    @Override
    public WidgetManager getWidgetManager(final GBDevice device) {
        return new XiaomiWidgetManager(device);
    }

    protected static Prefs getPrefs(final GBDevice device) {
        return new Prefs(GBApplication.getDeviceSpecificSharedPrefs(device.getAddress()));
    }

    public boolean supportsMultipleWeatherLocations() {
        return false;
    }

    public boolean supports(final GBDevice device, final String feature) {
        return getPrefs(device).getBoolean(feature, false);
    }
}
