/*  Copyright (C) 2018-2024 Andreas Shimokawa, Daniel Dakhno, Daniele Gobbetti,
    José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband3;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband3.MiBand3Support;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class MiBand3Coordinator extends HuamiCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(
                HuamiConst.MI_BAND3_NAME + "|" + HuamiConst.MI_BAND3_NAME_2,
                Pattern.CASE_INSENSITIVE
        );
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        final MiBand3FWInstallHandler handler = new MiBand3FWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    public static String getNightMode(final String deviceAddress) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(deviceAddress));

        return prefs.getString(MiBandConst.PREF_NIGHT_MODE, MiBandConst.PREF_NIGHT_MODE_OFF);
    }

    public static Date getNightModeStart(final String deviceAddress) {
        return getTimePreference(MiBandConst.PREF_NIGHT_MODE_START, "16:00", deviceAddress);
    }

    public static Date getNightModeEnd(final String deviceAddress) {
        return getTimePreference(MiBandConst.PREF_NIGHT_MODE_END, "07:00", deviceAddress);
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();

        final List<Integer> generic = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.GENERIC);
        generic.add(R.xml.devicesettings_wearlocation);
        final List<Integer> dateTime = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DATE_TIME);
        dateTime.add(R.xml.devicesettings_timeformat);
        dateTime.add(R.xml.devicesettings_dateformat);
        final List<Integer> display = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DISPLAY);
        display.add(R.xml.devicesettings_miband3);
        display.add(R.xml.devicesettings_nightmode);
        display.add(R.xml.devicesettings_liftwrist_display);
        display.add(R.xml.devicesettings_donotdisturb_lift_wrist);
        display.add(R.xml.devicesettings_swipeunlock);
        final List<Integer> health = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.HEALTH);
        health.add(R.xml.devicesettings_heartrate_sleep);
        health.add(R.xml.devicesettings_inactivity_dnd);
        health.add(R.xml.devicesettings_goal_notification);
        final List<Integer> notifications = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.NOTIFICATIONS);
        notifications.add(R.xml.devicesettings_donotdisturb_withauto);
        notifications.add(R.xml.devicesettings_phone_silent_mode);
        notifications.add(R.xml.devicesettings_transliteration);
        final List<Integer> calendar = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALENDAR);
        calendar.add(R.xml.devicesettings_sync_calendar);
        calendar.add(R.xml.devicesettings_reserve_reminders_calendar);
        final List<Integer> connection = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CONNECTION);
        connection.add(R.xml.devicesettings_expose_hr_thirdparty);
        connection.add(R.xml.devicesettings_bt_connected_advertisement);
        connection.add(R.xml.devicesettings_device_actions);
        connection.add(R.xml.devicesettings_overwrite_settings_on_connection);
        final List<Integer> developer = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.DEVELOPER);
        developer.add(R.xml.devicesettings_huami2021_fetch_operation_time_unit);

        return deviceSpecificSettings;
    }

    @Override
    public String[] getSupportedLanguageSettings(final GBDevice device) {
        return new String[]{
                "auto",
                "ar_SA",
                "de_DE",
                "en_US",
                "es_ES",
                "fr_FR",
                "id_ID",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "pt_PT",
                "nl_NL",
                "pl_PL",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CN",
                "zh_TW",
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MiBand3Support.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_miband3;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miband2;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_miband2_disabled;
    }
}
