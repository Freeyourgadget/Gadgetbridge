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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public abstract class Huami2021Coordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(Huami2021Coordinator.class);

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        // TODO: One-shot HR measures are not working, so let's disable this for now
        return false;
    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public boolean supportsActivityTracking() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsActivityTracks() {
        // TODO: It's supported by the devices, but not yet implemented
        return false;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getWorldClocksSlotCount() {
        // TODO: It's supported, but not implemented - even in the official app
        return 0;
    }

    @Override
    public boolean supportsCalendarEvents() {
        return true;
    }

    @Override
    public SampleProvider<? extends AbstractActivitySample> getSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO: It's supported by the devices, but not yet implemented
        return null;
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
    public int getReminderSlotCount() {
        return 50;
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "de_DE",
                "en_US",
                "es_ES",
                "fr_FR",
                "it_IT",
                "nl_NL",
                "pt_PT",
                "tr_TR",
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
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_header_time,
                //R.xml.devicesettings_timeformat,
                R.xml.devicesettings_dateformat_2,
                // TODO R.xml.devicesettings_world_clocks,

                R.xml.devicesettings_header_display,
                R.xml.devicesettings_huami2021_displayitems,
                R.xml.devicesettings_huami2021_shortcuts,
                R.xml.devicesettings_nightmode,
                R.xml.devicesettings_liftwrist_display_sensitivity,
                R.xml.devicesettings_password,
                R.xml.devicesettings_always_on_display,
                R.xml.devicesettings_screen_timeout_5_to_15,
                R.xml.devicesettings_screen_brightness,

                R.xml.devicesettings_header_health,
                R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2,
                R.xml.devicesettings_inactivity_dnd_no_threshold,
                R.xml.devicesettings_goal_notification,

                R.xml.devicesettings_header_workout,
                R.xml.devicesettings_workout_start_on_phone,
                R.xml.devicesettings_workout_send_gps_to_band,

                R.xml.devicesettings_header_notifications,
                R.xml.devicesettings_vibrationpatterns,
                R.xml.devicesettings_donotdisturb_withauto_and_always,
                R.xml.devicesettings_screen_on_on_notifications,
                R.xml.devicesettings_autoremove_notifications,
                R.xml.devicesettings_canned_reply_16,
                R.xml.devicesettings_transliteration,

                R.xml.devicesettings_header_calendar,
                R.xml.devicesettings_sync_calendar,

                R.xml.devicesettings_header_other,
                R.xml.devicesettings_device_actions_without_not_wear,

                R.xml.devicesettings_header_connection,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_bt_connected_advertisement,
                R.xml.devicesettings_high_mtu,
        };
    }

    @Override
    public int[] getSupportedDeviceSpecificAuthenticationSettings() {
        return new int[]{
                R.xml.devicesettings_pairingkey
        };
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new Huami2021SettingsCustomizer(device);
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }
}
