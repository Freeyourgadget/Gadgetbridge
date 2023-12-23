/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsUtils;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsAgpsInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsGpxRouteInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiHeartRateManualSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiHeartRateMaxSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiHeartRateRestingSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiPaiSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiSleepRespiratoryRateSampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiSpo2SampleDao;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiStressSampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuami2021FWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsAlexaService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsContactsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLogsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsLoyaltyCardService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsRemindersService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsShortcutCardsService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiLanguageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsPhoneService;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public abstract class Huami2021Coordinator extends HuamiCoordinator {
    public abstract AbstractHuami2021FWInstallHandler createFwInstallHandler(final Uri uri, final Context context);

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        if (supportsAgpsUpdates()) {
            final ZeppOsAgpsInstallHandler agpsInstallHandler = new ZeppOsAgpsInstallHandler(uri, context);
            if (agpsInstallHandler.isValid()) {
                return agpsInstallHandler;
            }
        }

        if (supportsGpxUploads()) {
            final ZeppOsGpxRouteInstallHandler gpxRouteInstallHandler = new ZeppOsGpxRouteInstallHandler(uri, context);
            if (gpxRouteInstallHandler.isValid()) {
                return gpxRouteInstallHandler;
            }
        }

        final AbstractHuami2021FWInstallHandler handler = createFwInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsScreenshots() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsManualHeartRateMeasurement(final GBDevice device) {
        // TODO: It should be supported, but not yet properly implemented
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public boolean supportsRemSleep() {
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
    public boolean supportsSpo2() {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats() {
        return true;
    }

    @Override
    public boolean supportsPai() {
        return true;
    }

    @Override
    public boolean supportsSleepRespiratoryRate() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 20; // as enforced by Zepp
    }

    @Override
    public int getWorldClocksLabelLength() {
        return 30; // at least
    }

    @Override
    public boolean supportsDisabledWorldClocks() {
        return true;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        return experimentalFeatures(device);
    }

    @Override
    public Class<? extends Activity> getAppsManagementActivity() {
        return AppManagerActivity.class;
    }

    @Override
    public File getAppCacheDir() throws IOException {
        return new File(FileUtils.getExternalFilesDir(), "zepp-os-app-cache");
    }

    @Override
    public String getAppCacheSortFilename() {
        return "zepp-os-app-cache-order.txt";
    }

    @Override
    public String getAppFileExtension() {
        return ".zip";
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
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws GBException {
        final Long deviceId = device.getId();

        session.getHuamiExtendedActivitySampleDao().queryBuilder()
                .where(HuamiExtendedActivitySampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiStressSampleDao().queryBuilder()
                .where(HuamiStressSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiSpo2SampleDao().queryBuilder()
                .where(HuamiSpo2SampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiHeartRateManualSampleDao().queryBuilder()
                .where(HuamiHeartRateManualSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiHeartRateMaxSampleDao().queryBuilder()
                .where(HuamiHeartRateMaxSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiHeartRateRestingSampleDao().queryBuilder()
                .where(HuamiHeartRateRestingSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiPaiSampleDao().queryBuilder()
                .where(HuamiPaiSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();

        session.getHuamiSleepRespiratoryRateSampleDao().queryBuilder()
                .where(HuamiSleepRespiratoryRateSampleDao.Properties.DeviceId.eq(deviceId))
                .buildDelete().executeDeleteWithoutDetachingEntities();
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        return new HuamiExtendedSampleProvider(device, session);
    }

    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device) {
        return new Huami2021ActivitySummaryParser();
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        // All alarms snooze by default, there doesn't seem to be a flag that disables it
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(final GBDevice device) {
        return true;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return ZeppOsRemindersService.getSlotCount(getPrefs(device));
    }

    @Override
    public int getCannedRepliesSlotCount(final GBDevice device) {
        return 16;
    }

    @Override
    public int getContactsSlotCount(final GBDevice device) {
        return getPrefs(device).getInt(ZeppOsContactsService.PREF_CONTACTS_SLOT_COUNT, 0);
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        // Return all known languages by default. Unsupported languages will be removed by Huami2021SettingsCustomizer
        final List<String> allLanguages = new ArrayList<>(HuamiLanguageType.idLookup.keySet());
        allLanguages.add(0, "auto");
        return allLanguages.toArray(new String[0]);
    }

    @Override
    public PasswordCapabilityImpl.Mode getPasswordCapability() {
        return PasswordCapabilityImpl.Mode.NUMBERS_6;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        // Return all known by default. Unsupported will be removed by Huami2021SettingsCustomizer
        return Arrays.asList(HeartRateCapability.MeasurementInterval.values());
    }

    /**
     * Returns a superset of all settings supported by Zepp OS Devices. Unsupported settings are removed
     * by {@link Huami2021SettingsCustomizer}.
     */
    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        final List<Integer> settings = new ArrayList<>();

        //
        // Apps
        // TODO: These should go somewhere else
        //
        settings.add(R.xml.devicesettings_header_apps);
        if (ZeppOsLoyaltyCardService.isSupported(getPrefs(device))) {
            settings.add(R.xml.devicesettings_loyalty_cards);
        }

        //
        // Time
        //
        settings.add(R.xml.devicesettings_header_time);
        //settings.add(R.xml.devicesettings_timeformat);
        settings.add(R.xml.devicesettings_dateformat_2);
        if (getWorldClocksSlotCount() > 0) {
            settings.add(R.xml.devicesettings_world_clocks);
        }

        //
        // Display
        //
        settings.add(R.xml.devicesettings_header_display);
        settings.add(R.xml.devicesettings_huami2021_displayitems);
        settings.add(R.xml.devicesettings_huami2021_shortcuts);
        if (supportsControlCenter()) {
            settings.add(R.xml.devicesettings_huami2021_control_center);
        }
        if (supportsShortcutCards(device)) {
            settings.add(R.xml.devicesettings_huami2021_shortcut_cards);
        }
        settings.add(R.xml.devicesettings_nightmode);
        settings.add(R.xml.devicesettings_sleep_mode);
        settings.add(R.xml.devicesettings_liftwrist_display_sensitivity_with_smart);
        settings.add(R.xml.devicesettings_password);
        settings.add(R.xml.devicesettings_huami2021_watchface);
        settings.add(R.xml.devicesettings_always_on_display);
        settings.add(R.xml.devicesettings_screen_timeout);
        if (supportsAutoBrightness(device)) {
            settings.add(R.xml.devicesettings_screen_brightness_withauto);
        } else {
            settings.add(R.xml.devicesettings_screen_brightness);
        }

        //
        // Health
        //
        settings.add(R.xml.devicesettings_header_health);
        settings.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2);
        settings.add(R.xml.devicesettings_inactivity_dnd_no_threshold);
        settings.add(R.xml.devicesettings_goal_notification);

        //
        // Workout
        //
        settings.add(R.xml.devicesettings_header_workout);
        if (hasGps(device)) {
            settings.add(R.xml.devicesettings_gps_agps);
        } else {
            // If the device has GPS, it doesn't report workout start/end to the phone
            settings.add(R.xml.devicesettings_workout_start_on_phone);
            settings.add(R.xml.devicesettings_workout_send_gps_to_band);
        }
        settings.add(R.xml.devicesettings_workout_keep_screen_on);
        settings.add(R.xml.devicesettings_workout_detection);

        //
        // Notifications
        //
        settings.add(R.xml.devicesettings_header_notifications);
        if (supportsBluetoothPhoneCalls(device)) {
            settings.add(R.xml.devicesettings_phone_calls_watch_pair);
        } else {
            settings.add(R.xml.devicesettings_display_caller);
        }
        settings.add(R.xml.devicesettings_sound_and_vibration);
        settings.add(R.xml.devicesettings_vibrationpatterns);
        settings.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
        settings.add(R.xml.devicesettings_send_app_notifications);
        settings.add(R.xml.devicesettings_screen_on_on_notifications);
        settings.add(R.xml.devicesettings_autoremove_notifications);
        settings.add(R.xml.devicesettings_canned_reply_16);
        settings.add(R.xml.devicesettings_transliteration);

        //
        // Calendar
        //
        settings.add(R.xml.devicesettings_header_calendar);
        settings.add(R.xml.devicesettings_sync_calendar);

        //
        // Other
        //
        settings.add(R.xml.devicesettings_header_other);
        if (getContactsSlotCount(device) > 0) {
            settings.add(R.xml.devicesettings_contacts);
        }
        settings.add(R.xml.devicesettings_offline_voice);
        settings.add(R.xml.devicesettings_device_actions_without_not_wear);
        settings.add(R.xml.devicesettings_phone_silent_mode);
        settings.add(R.xml.devicesettings_buttonactions_upper_long);
        settings.add(R.xml.devicesettings_buttonactions_lower_short);
        settings.add(R.xml.devicesettings_weardirection);
        settings.add(R.xml.devicesettings_camera_remote);
        settings.add(R.xml.devicesettings_morning_updates);

        //
        // Connection
        //
        settings.add(R.xml.devicesettings_header_connection);
        settings.add(R.xml.devicesettings_expose_hr_thirdparty);
        settings.add(R.xml.devicesettings_bt_connected_advertisement);
        settings.add(R.xml.devicesettings_high_mtu);

        //
        // Developer
        //
        settings.add(R.xml.devicesettings_header_developer);
        if (ZeppOsLogsService.isSupported(getPrefs(device))) {
            settings.add(R.xml.devicesettings_app_logs_start_stop);
        }
        if (supportsAlexa(device)) {
            settings.add(R.xml.devicesettings_huami2021_alexa);
        }
        if (supportsWifiHotspot(device)) {
            settings.add(R.xml.devicesettings_wifi_hotspot);
        }
        if (supportsFtpServer(device)) {
            settings.add(R.xml.devicesettings_ftp_server);
        }
        settings.add(R.xml.devicesettings_keep_activity_data_on_device);
        settings.add(R.xml.devicesettings_huami2021_fetch_operation_time_unit);

        return ArrayUtils.toPrimitive(settings.toArray(new Integer[0]));
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_pairingkey
        };
    }

    @Override
    public List<HuamiVibrationPatternNotificationType> getVibrationPatternNotificationTypes(final GBDevice device) {
        final List<HuamiVibrationPatternNotificationType> notificationTypes = new ArrayList<>(Arrays.asList(
                HuamiVibrationPatternNotificationType.APP_ALERTS,
                HuamiVibrationPatternNotificationType.INCOMING_CALL,
                HuamiVibrationPatternNotificationType.INCOMING_SMS,
                HuamiVibrationPatternNotificationType.GOAL_NOTIFICATION,
                HuamiVibrationPatternNotificationType.ALARM,
                HuamiVibrationPatternNotificationType.IDLE_ALERTS
        ));

        if (getReminderSlotCount(device) > 0) {
            notificationTypes.add(HuamiVibrationPatternNotificationType.EVENT_REMINDER);
        }

        if (!supportsContinuousFindDevice()) {
            notificationTypes.add(HuamiVibrationPatternNotificationType.FIND_BAND);
        }

        if (supportsToDoList()) {
            notificationTypes.add(HuamiVibrationPatternNotificationType.SCHEDULE);
            notificationTypes.add(HuamiVibrationPatternNotificationType.TODO_LIST);
        }

        return notificationTypes;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new Huami2021SettingsCustomizer(device, getVibrationPatternNotificationTypes(device));
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    public boolean supportsContinuousFindDevice() {
        // TODO: Auto-detect continuous find device?
        return false;
    }

    public boolean supportsAgpsUpdates() {
        return true;
    }

    /**
     * true for Zepp OS 2.0+, false for Zepp OS 1
     */
    public boolean sendAgpsAsFileTransfer() {
        return true;
    }

    public boolean supportsGpxUploads() {
        return false;
    }

    public boolean supportsControlCenter() {
        // TODO: Auto-detect control center?
        return false;
    }

    public boolean supportsToDoList() {
        // TODO: Not yet implemented
        // TODO: When implemented, query the capability like reminders
        return false;
    }

    public boolean mainMenuHasMoreSection() {
        // Devices that have a control center don't seem to have a "more" section in the main menu
        return !supportsControlCenter();
    }

    public boolean supportsWifiHotspot(final GBDevice device) {
        return false;
    }

    public boolean supportsFtpServer(final GBDevice device) {
        return false;
    }

    public boolean hasGps(final GBDevice device) {
        return supportsConfig(device, ZeppOsConfigService.ConfigArg.WORKOUT_GPS_PRESET);
    }

    public boolean supportsAutoBrightness(final GBDevice device) {
        return supportsConfig(device, ZeppOsConfigService.ConfigArg.SCREEN_AUTO_BRIGHTNESS);
    }

    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return ZeppOsPhoneService.isSupported(getPrefs(device));
    }

    public boolean supportsShortcutCards(final GBDevice device) {
        return ZeppOsShortcutCardsService.isSupported(getPrefs(device));
    }

    public boolean supportsAlexa(final GBDevice device) {
        return experimentalFeatures(device) && ZeppOsAlexaService.isSupported(getPrefs(device));
    }

    private boolean supportsConfig(final GBDevice device, final ZeppOsConfigService.ConfigArg config) {
        return ZeppOsConfigService.deviceHasConfig(getPrefs(device), config);
    }

    public static boolean deviceHasConfig(final Prefs devicePrefs, final ZeppOsConfigService.ConfigArg config) {
        return devicePrefs.getBoolean(DeviceSettingsUtils.getPrefKnownConfig(config.name()), false);
    }

    public static boolean experimentalFeatures(final GBDevice device) {
        return getPrefs(device).getBoolean("zepp_os_experimental_features", false);
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        final byte[] authKeyBytes = authKey.trim().getBytes();
        return authKeyBytes.length == 32 || (authKey.trim().startsWith("0x") && authKeyBytes.length == 34);
    }
}
