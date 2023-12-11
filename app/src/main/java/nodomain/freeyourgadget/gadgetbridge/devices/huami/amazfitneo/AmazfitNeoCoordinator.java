/*  Copyright (C) 2020 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.amazfitneo;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.capabilities.HeartRateCapability;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.amazfitneo.AmazfitNeoSupport;

public class AmazfitNeoCoordinator extends HuamiCoordinator {
    private static final Logger LOG = LoggerFactory.getLogger(AmazfitNeoCoordinator.class);

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(HuamiConst.AMAZFIT_NEO_NAME, Pattern.CASE_INSENSITIVE);
    }

    @Override
    public InstallHandler findInstallHandler(Uri uri, Context context) {
        AmazfitNeoFWInstallHandler handler = new AmazfitNeoFWInstallHandler(uri, context);
        return handler.isValid() ? handler : null;
    }

    @Override
    public boolean supportsHeartRateMeasurement(GBDevice device) {
        return true;
    }

    @Override
    public boolean supportsPai(){
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public boolean supportsActivityTracks() {
        return false;
    }

    @Override
    public int getWorldClocksSlotCount() {
        return 20; // max in Zepp app
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return 0; // Neo does not support reminders
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_amazfitneo,
                R.xml.devicesettings_wearlocation,
                R.xml.devicesettings_heartrate_sleep_activity,
                R.xml.devicesettings_goal_notification,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_world_clocks,
                R.xml.devicesettings_liftwrist_display,
                R.xml.devicesettings_inactivity_dnd,
                R.xml.devicesettings_hourly_chime,
                R.xml.devicesettings_expose_hr_thirdparty,
                R.xml.devicesettings_bt_connected_advertisement,
                R.xml.devicesettings_device_actions,
                R.xml.devicesettings_phone_silent_mode,
                R.xml.devicesettings_high_mtu,
                R.xml.devicesettings_overwrite_settings_on_connection,
                R.xml.devicesettings_huami2021_fetch_operation_time_unit,
                R.xml.devicesettings_transliteration
        };
    }

    @Override
    public int getBondingStyle() {
        return BONDING_STYLE_REQUIRE_KEY;
    }

    @Override
    public List<HeartRateCapability.MeasurementInterval> getHeartRateMeasurementIntervals() {
        return Arrays.asList(
                HeartRateCapability.MeasurementInterval.OFF,
                HeartRateCapability.MeasurementInterval.MINUTES_1,
                HeartRateCapability.MeasurementInterval.MINUTES_5,
                HeartRateCapability.MeasurementInterval.MINUTES_10,
                HeartRateCapability.MeasurementInterval.MINUTES_30
        );
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return AmazfitNeoSupport.class;
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_amazfit_neo;
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
