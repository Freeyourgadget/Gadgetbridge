/*  Copyright (C) 2018-2024 Andreas Shimokawa, Damien Gaignon, Daniel Dakhno,
    Daniele Gobbetti, José Rebelo, Petr Vaněk, Sebastian Kranz

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
package nodomain.freeyourgadget.gadgetbridge.devices.zetime;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsCustomizer;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.zetime.ZeTimeDeviceSupport;

public class ZeTimeCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("ZeTime.*");
    }

    @Override
    public int getAlarmSlotCount(final GBDevice device) {
        return 3; // FIXME - check the real value
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
    public String getManufacturer() {
        return "MyKronoz";
    }

    @Override
    public boolean supportsActivityTracking() {
        return true;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityDataFetching() {
        return true;
    }

    @Override
    public SampleProvider<? extends ActivitySample> getSampleProvider(GBDevice device, DaoSession session) {
        return new ZeTimeSampleProvider(device, session);
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {

    }

    @Override
    public boolean supportsRealtimeData() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_NONE;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return  true;
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        final List<Integer> generic = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.GENERIC);
        generic.add(R.xml.devicesettings_wearlocation);

        final List<Integer> datetime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
        datetime.add(R.xml.devicesettings_timeformat);
        datetime.add(R.xml.devicesettings_zetime_date_format);

        final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
        display.add(R.xml.devicesettings_zetime_display);

        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_zetime_health);

        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_donotdisturb_no_auto);
        notifications.add(R.xml.devicesettings_zetime_vibrationpatterns);
        notifications.add(R.xml.devicesettings_transliteration);

        final List<Integer> calendar = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALENDAR);
        calendar.add(R.xml.devicesettings_sync_calendar);

        final List<Integer> activityInfo = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.ACTIVITY_INFO);
        activityInfo.add(R.xml.devicesettings_zetime_activity_info);

        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_keep_activity_data_on_device);

        return deviceSpecificSettings;
    }

    @Override
    public DeviceSpecificSettingsCustomizer getDeviceSpecificSettingsCustomizer(final GBDevice device) {
        return new ZeTimeSettingsCustomizer(device);
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_15,
                HeartRateCapability.MeasurementInterval.MINUTES_30,
                HeartRateCapability.MeasurementInterval.MINUTES_45,
                HeartRateCapability.MeasurementInterval.HOUR_1
        );
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "zh_CN",
                "zh_TW",
                "en_US",
                "es_ES",
                "de_DE",
                "it_IT",
                "fr_FR",
                "pt_PT",
                "nl_NL",
                "sv_SE",
                "pl_PL",
                "cs_CZ",
                "el_GR",
                "ru_RU",
                "he_IL",
                "ar_SA",
                "ja_JP",
                "ko_KO",
                "th_TH",
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return ZeTimeDeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_mykronoz_zetime;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_zetime;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_zetime_disabled;
    }
}
