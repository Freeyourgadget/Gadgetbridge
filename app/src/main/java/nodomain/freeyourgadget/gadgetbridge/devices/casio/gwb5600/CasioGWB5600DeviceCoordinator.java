/*  Copyright (C) 2023-2024 Daniel Dakhno, Johannes Krude, José Rebelo

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
/*  Based on code from BlueWatcher, https://github.com/masterjc/bluewatcher */
package nodomain.freeyourgadget.gadgetbridge.devices.casio.gwb5600;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

import nodomain.freeyourgadget.gadgetbridge.devices.casio.Casio2C2DDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.gwb5600.CasioGWB5600DeviceSupport;

public class CasioGWB5600DeviceCoordinator extends Casio2C2DDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("CASIO GW-B5600");
    }

    @Override
    public int getBondingStyle(){
        return BONDING_STYLE_BOND;
    }


    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_dateformat_day_month_order,
                R.xml.devicesettings_operating_sounds,
                R.xml.devicesettings_hourly_chime_enable,
                R.xml.devicesettings_autolight,
                R.xml.devicesettings_light_duration_longer,
                R.xml.devicesettings_power_saving,
                R.xml.devicesettings_casio_connection_duration,
                R.xml.devicesettings_time_sync,

                // timer
                // reminder
                // world time
        };
    }

    @Override
    public String[] getSupportedLanguageSettings(GBDevice device) {
        return new String[]{
                "auto",
                "en_US",
                "es_ES",
                "fr_FR",
                "de_DE",
                "it_IT",
                "ru_RU",
        };
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) {
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 5;
    }

    @Override
    public int getMaximumReminderMessageLength() {
        return 18;
    }

    @Override
    public int getReminderSlotCount(final GBDevice device) {
        return 5;
    }

    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return CasioGWB5600DeviceSupport.class;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_casiogwb5600;
    }

}
