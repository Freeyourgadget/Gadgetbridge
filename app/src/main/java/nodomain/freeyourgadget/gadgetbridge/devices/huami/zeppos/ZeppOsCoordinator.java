/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.appmanager.AppManagerActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.Vo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.WorkoutVo2MaxSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiExtendedSampleProvider;
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
import nodomain.freeyourgadget.gadgetbridge.model.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsFwInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.ZeppOsSupport;
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
import nodomain.freeyourgadget.gadgetbridge.devices.SleepAsAndroidFeature;

public abstract class ZeppOsCoordinator extends HuamiCoordinator {
    public abstract String getDeviceBluetoothName();

    public abstract Set<Integer> getDeviceSources();

    protected Map<Integer, String> getCrcMap() {
        // A map from CRC16 to human-readable version for flashable files
        return Collections.emptyMap();
    }

    @Override
    public String getManufacturer() {
        // Actual manufacturer is Huami
        return "Amazfit";
    }

    @Override
    protected final Pattern getSupportedDeviceName() {
        // Most devices use the exact bluetooth name
        // Some devices have a " XXXX" suffix with the last 4 digits of mac address (eg. Mi Band 7)
        // *However*, some devices broadcast a 2nd bluetooth device with "-XXXX" suffix, which I believe
        // is only used for calls, and Gadgetbridge can't use for pairing, but I was not yet able to
        // fully confirm this, so we still recognize them.
        return Pattern.compile("^" + getDeviceBluetoothName() + "([- ][A-Z0-9]{4})?$");
    }

    @NonNull
    @Override
    public final Class<? extends DeviceSupport> getDeviceSupportClass() {
        return ZeppOsSupport.class;
    }

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

        final ZeppOsFwInstallHandler handler = new ZeppOsFwInstallHandler(
                uri,
                context,
                getDeviceBluetoothName(),
                getDeviceSources()
        );
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsScreenshots(final GBDevice device) {
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
    public boolean supportsSpo2(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsVO2Max() {
        return true;
    }

    @Override
    public boolean supportsVO2MaxRunning() {
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
    public boolean supportsSleepAsAndroid() {
        return true;
    }

    @Override
    public Set<SleepAsAndroidFeature> getSleepAsAndroidFeatures() {
        return EnumSet.of(SleepAsAndroidFeature.ACCELEROMETER, SleepAsAndroidFeature.HEART_RATE, SleepAsAndroidFeature.ALARMS, SleepAsAndroidFeature.NOTIFICATIONS);
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
    public Vo2MaxSampleProvider<? extends Vo2MaxSample> getVo2MaxSampleProvider(final GBDevice device, final DaoSession session) {
        return new WorkoutVo2MaxSampleProvider(device, session);
    }

    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device, final Context context) {
        return new ZeppOsActivitySummaryParser(context);
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        // All alarms snooze by default, there doesn't seem to be a flag that disables it
        return false;
    }

    @Override
    public boolean supportsSmartWakeup(final GBDevice device, int position) {
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
     * by {@link ZeppOsSettingsCustomizer}.
     */
    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        //
        // Apps
        // TODO: These should go somewhere else
        //
        if (ZeppOsLoyaltyCardService.isSupported(getPrefs(device))) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_loyalty_cards);
        }

        //
        // Time
        //
        final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
        //dateTime.add(R.xml.devicesettings_timeformat);
        dateTime.add(R.xml.devicesettings_dateformat_2);
        if (getWorldClocksSlotCount() > 0) {
            dateTime.add(R.xml.devicesettings_world_clocks);
        }

        //
        // Display
        //
        final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
        display.add(R.xml.devicesettings_huami2021_displayitems);
        display.add(R.xml.devicesettings_huami2021_shortcuts);
        if (supportsControlCenter()) {
            display.add(R.xml.devicesettings_huami2021_control_center);
        }
        if (supportsShortcutCards(device)) {
            display.add(R.xml.devicesettings_huami2021_shortcut_cards);
        }
        display.add(R.xml.devicesettings_nightmode);
        display.add(R.xml.devicesettings_sleep_mode);
        display.add(R.xml.devicesettings_liftwrist_display_sensitivity_with_smart);
        display.add(R.xml.devicesettings_password);
        display.add(R.xml.devicesettings_huami2021_watchface);
        display.add(R.xml.devicesettings_always_on_display);
        display.add(R.xml.devicesettings_screen_timeout);
        if (supportsAutoBrightness(device)) {
            display.add(R.xml.devicesettings_screen_brightness_withauto);
        } else {
            display.add(R.xml.devicesettings_screen_brightness);
        }

        //
        // Health
        //
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_inactivity_dnd_no_threshold);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_goal_notification);

        //
        // Workout
        //
        final List<Integer> workout = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.WORKOUT);
        if (hasGps(device)) {
            workout.add(R.xml.devicesettings_gps_agps);
        } else {
            // If the device has GPS, it doesn't report workout start/end to the phone
            workout.add(R.xml.devicesettings_workout_start_on_phone);
            workout.add(R.xml.devicesettings_workout_send_gps_to_band);
        }
        workout.add(R.xml.devicesettings_workout_keep_screen_on);
        workout.add(R.xml.devicesettings_workout_detection);

        //
        // Notifications
        //
        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        if (supportsBluetoothPhoneCalls(device)) {
            notifications.add(R.xml.devicesettings_phone_calls_watch_pair);
        } else {
            notifications.add(R.xml.devicesettings_display_caller);
        }
        notifications.add(R.xml.devicesettings_sound_and_vibration);
        notifications.add(R.xml.devicesettings_vibrationpatterns);
        notifications.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
        notifications.add(R.xml.devicesettings_send_app_notifications);
        notifications.add(R.xml.devicesettings_screen_on_on_notifications);
        notifications.add(R.xml.devicesettings_autoremove_notifications);
        notifications.add(R.xml.devicesettings_canned_reply_16);
        notifications.add(R.xml.devicesettings_transliteration);

        //
        // Calendar
        //
        deviceSpecificSettings.addRootScreen(
                DeviceSpecificSettingsScreen.CALENDAR,
                R.xml.devicesettings_sync_calendar
        );

        //
        // Other
        //
        if (getContactsSlotCount(device) > 0) {
            deviceSpecificSettings.addRootScreen(R.xml.devicesettings_contacts);
        }
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_offline_voice);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_device_actions_without_not_wear);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_phone_silent_mode);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_buttonactions_upper_long);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_buttonactions_lower_short);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_weardirection);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_camera_remote);
        deviceSpecificSettings.addRootScreen(R.xml.devicesettings_morning_updates);

        //
        // Connection
        //
        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_expose_hr_thirdparty);
        connection.add(R.xml.devicesettings_bt_connected_advertisement);
        connection.add(R.xml.devicesettings_high_mtu);

        //
        // Developer
        //
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        if (ZeppOsLogsService.isSupported(getPrefs(device))) {
            developer.add(R.xml.devicesettings_app_logs_start_stop);
        }
        if (supportsAlexa(device)) {
            developer.add(R.xml.devicesettings_huami2021_alexa);
        }
        if (supportsWifiHotspot(device)) {
            developer.add(R.xml.devicesettings_wifi_hotspot);
        }
        if (supportsFtpServer(device)) {
            developer.add(R.xml.devicesettings_ftp_server);
        }
        developer.add(R.xml.devicesettings_keep_activity_data_on_device);
        developer.add(R.xml.devicesettings_huami2021_fetch_operation_time_unit);

        return deviceSpecificSettings;
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
        return new ZeppOsSettingsCustomizer(device, getVibrationPatternNotificationTypes(device));
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

    public static boolean experimentalFeatures(final GBDevice device) {
        return getPrefs(device).getBoolean("zepp_os_experimental_features", false);
    }

    @Override
    public boolean validateAuthKey(final String authKey) {
        final byte[] authKeyBytes = authKey.trim().getBytes();
        return authKeyBytes.length == 32 || (authKey.trim().startsWith("0x") && authKeyBytes.length == 34);
    }
}
