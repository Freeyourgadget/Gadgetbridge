/*  Copyright (C) 2020-2024 Andreas Shimokawa, Daniel Dakhno, Joel Beckmeyer,
    José Rebelo, Petr Vaněk, TinfoilSubmarine

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitbipu;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitbipu.AmazfitBipUSupport;

public class AmazfitBipUCoordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitBipUCoordinator.class);

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Amazfit Bip U", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        AmazfitBipUFWInstallHandler handler = new AmazfitBipUFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsMusicInfo() {
        return true;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        return true;
    }

    @Override
    public boolean supportsAlarmSnoozing() {
        // All alarms snooze by default, there doesn't seem to be a flag that disables it
        return false;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return 0;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 20; // as enforced by Mi Fit
    }

    @Override
    public int getWorldClocksLabelLength() {
        return 30; // at least
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_amazfitbipu,
                R.xml.devicesettings_vibrationpatterns,
                //R.xml.devicesettings_canned_dismisscall_16,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_world_clocks,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_heartrate_sleep_alert_activity_stress,
                R.xml.devicesettings_goal_notification,
                R.xml.devicesettings_custom_emoji_font,
                R.xml.devicesettings_liftwrist_display_sensitivity,
                R.xml.devicesettings_inactivity_dnd,
                R.xml.devicesettings_workout_start_on_phone,
                R.xml.devicesettings_workout_send_gps_to_band,
                R.xml.devicesettings_sync_calendar,
                R.xml.devicesettings_reserve_reminders_calendar,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_bt_connected_advertisement,
                R.xml.devicesettings_high_mtu,
                R.xml.devicesettings_device_actions,
                R.xml.devicesettings_phone_silent_mode,
                R.xml.devicesettings_overwrite_settings_on_connection,
                R.xml.devicesettings_huami2021_fetch_operation_time_unit,
                R.xml.devicesettings_transliteration
        };
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "cs_CZ",
                "de_DE",
                "el_GR",
                "en_US",
                "es_ES",
                "fr_FR",
                "id_ID",
                "it_IT",
                "ja_JP",
                "ko_KO",
                "nl_NL",
                "pl_PL",
                "pt_BR",
                "ru_RU",
                "th_TH",
                "tr_TR",
                "uk_UA",
                "vi_VN",
                "zh_CH",
                "zh_TW",
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return AmazfitBipUSupport.class;
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_bipu;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_amazfit_bip_disabled;
    }
}
