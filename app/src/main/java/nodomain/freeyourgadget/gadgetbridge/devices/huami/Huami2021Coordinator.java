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

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.HuamiExtendedActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.AbstractHuami2021FWInstallHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.zeppos.services.ZeppOsConfigService;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiLanguageType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiVibrationPatternNotificationType;

public abstract class Huami2021Coordinator extends HuamiCoordinator {
    @Override
    public abstract AbstractHuami2021FWInstallHandler findInstallHandler(final Uri uri, final Context context);

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
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws GBException {
        final Long deviceId = device.getId();
        final QueryBuilder<?> qb = session.getHuamiExtendedActivitySampleDao().queryBuilder();
        qb.where(HuamiExtendedActivitySampleDao.Properties.DeviceId.eq(deviceId)).buildDelete().executeDeleteWithoutDetachingEntities();
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
        return getPrefs(device).getInt(Huami2021Service.REMINDERS_PREF_CAPABILITY, 0);
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
        settings.add(R.xml.devicesettings_nightmode);
        settings.add(R.xml.devicesettings_sleep_mode);
        settings.add(R.xml.devicesettings_liftwrist_display_sensitivity);
        settings.add(R.xml.devicesettings_password);
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
        settings.add(R.xml.devicesettings_workout_detection);

        //
        // Notifications
        //
        settings.add(R.xml.devicesettings_header_notifications);
        settings.add(R.xml.devicesettings_sound_and_vibration);
        settings.add(R.xml.devicesettings_vibrationpatterns);
        settings.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
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
        settings.add(R.xml.devicesettings_offline_voice);
        settings.add(R.xml.devicesettings_device_actions_without_not_wear);
        settings.add(R.xml.devicesettings_buttonactions_upper_long);
        settings.add(R.xml.devicesettings_buttonactions_lower_short);
        settings.add(R.xml.devicesettings_weardirection);

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

    public boolean hasGps(final GBDevice device) {
        return supportsConfig(device, ZeppOsConfigService.ConfigArg.WORKOUT_GPS_PRESET);
    }

    public boolean supportsAutoBrightness(final GBDevice device) {
        return supportsConfig(device, ZeppOsConfigService.ConfigArg.SCREEN_AUTO_BRIGHTNESS);
    }

    private boolean supportsConfig(final GBDevice device, final ZeppOsConfigService.ConfigArg config) {
        return ZeppOsConfigService.deviceHasConfig(getPrefs(device), config);
    }
}
