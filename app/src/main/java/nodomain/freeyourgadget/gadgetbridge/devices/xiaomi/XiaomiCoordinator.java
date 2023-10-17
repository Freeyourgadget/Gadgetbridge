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

import android.bluetooth.le.ScanFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.capabilities.password.PasswordCapabilityImpl;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.AbstractNotificationPattern;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;
import nodomain.freeyourgadget.gadgetbridge.model.SleepRespiratoryRateSample;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiLanguageType;

public abstract class XiaomiCoordinator extends AbstractBLEDeviceCoordinator {
    @NonNull
    @Override
    public Collection<? extends ScanFilter> createBLEScanFilters() {
        return super.createBLEScanFilters();
    }

    @Override
    protected void deleteDevice(@NonNull final GBDevice gbDevice,
                                @NonNull final Device device,
                                @NonNull final DaoSession session) throws GBException {
        // TODO, and fix on zepp
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(final GBDevice device, DaoSession session) {
        return new XiaomiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends StressSample> getStressSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiStressSampleProvider
        return super.getStressSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends Spo2Sample> getSpo2SampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiSpo2SampleProvider
        return super.getSpo2SampleProvider(device, session);
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
        // TODO XiaomiPaiSampleProvider
        return super.getPaiSampleProvider(device, session);
    }

    @Override
    public TimeSampleProvider<? extends SleepRespiratoryRateSample> getSleepRespiratoryRateSampleProvider(final GBDevice device, final DaoSession session) {
        // TODO XiaomiSleepRespiratoryRateSampleProvider
        return super.getSleepRespiratoryRateSampleProvider(device, session);
    }

    @Nullable
    @Override
    public ActivitySummaryParser getActivitySummaryParser(final GBDevice device) {
        return new XiaomiActivitySummaryParser();
    }

    @Override
    public boolean supportsFlashing() {
        return true;
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        // TODO the watch returns the slot count
        return 10;
    }

    @Override
    public boolean supportsSmartWakeup(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsAppsManagement(final GBDevice device) {
        // TODO maybe for watchfaces or widgets?
        return super.supportsAppsManagement(device);
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
    public boolean supportsSpo2() {
        return true;
    }

    @Override
    public boolean supportsHeartRateStats() {
        // TODO does it?
        return true;
    }

    @Override
    public boolean supportsPai() {
        // TODO does it?
        return true;
    }

    @Override
    public boolean supportsSleepRespiratoryRate() {
        // TODO does it?
        return true;
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        // TODO does it?
        return false;
    }

    @Override
    public boolean supportsAlarmDescription(final GBDevice device) {
        // TODO does it?
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
        // TODO fetch from watch
        return 50;
    }

    @Override
    public int getWorldClocksSlotCount() {
        // TODO how many?
        return 5;
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
        // TODO orchestrate
        return true;
    }

    @Override
    public String getManufacturer() {
        return "Xiaomi";
    }

    @Override
    public boolean supportsRealtimeData() {
        // TODO supports steps?
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

        // TODO review this
        
        //
        // Time
        //
        settings.add(R.xml.devicesettings_header_time);
        settings.add(R.xml.devicesettings_timeformat);
        settings.add(R.xml.devicesettings_dateformat_2);
        if (getWorldClocksSlotCount() > 0) {
            settings.add(R.xml.devicesettings_world_clocks);
        }

        //
        // Display
        //
        settings.add(R.xml.devicesettings_header_display);
        settings.add(R.xml.devicesettings_xiaomi_displayitems);
        settings.add(R.xml.devicesettings_password);

        //
        // Health
        //
        settings.add(R.xml.devicesettings_header_health);
        settings.add(R.xml.devicesettings_heartrate_sleep_alert_activity_stress_spo2);
        settings.add(R.xml.devicesettings_inactivity_dnd_no_threshold);
        settings.add(R.xml.devicesettings_sleep_mode_schedule);
        settings.add(R.xml.devicesettings_goal_notification);

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
        settings.add(R.xml.devicesettings_display_caller);
        settings.add(R.xml.devicesettings_vibrationpatterns);
        settings.add(R.xml.devicesettings_donotdisturb_withauto_and_always);
        settings.add(R.xml.devicesettings_screen_on_on_notifications);
        settings.add(R.xml.devicesettings_autoremove_notifications);
        settings.add(R.xml.devicesettings_canned_reply_16);

        //
        // Calendar
        //
        settings.add(R.xml.devicesettings_header_calendar);
        settings.add(R.xml.devicesettings_sync_calendar);

        //
        // Other
        //
        settings.add(R.xml.devicesettings_header_other);
        settings.add(R.xml.devicesettings_camera_remote);

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
        // TODO check which are supported
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
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.SMART,
                HeartRateCapability.MeasurementInterval.MINUTES_1,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_30
        );
    }

    @Override
    public boolean supportsNotificationVibrationPatterns() {
        // TODO maybe can used this
        return true;
    }

    @Override
    public boolean supportsNotificationVibrationRepetitionPatterns() {
        // TODO maybe can used this
        return true;
    }

    @Override
    public boolean supportsNotificationLedPatterns() {
        return false;
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationPatterns() {
        // TODO maybe can used this
        return new AbstractNotificationPattern[0];
    }

    @Override
    public AbstractNotificationPattern[] getNotificationVibrationRepetitionPatterns() {
        // TODO maybe can used this
        return new AbstractNotificationPattern[0];
    }

    @Override
    public AbstractNotificationPattern[] getNotificationLedPatterns() {
        return new AbstractNotificationPattern[0];
    }
}
