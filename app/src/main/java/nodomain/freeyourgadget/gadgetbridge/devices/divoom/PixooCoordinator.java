/*  Copyright (C) 2023 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.devices.divoom;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLEDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.InstallHandler;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.divoom.PixooSupport;

public class PixooCoordinator extends AbstractBLEDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Pixoo(-.+)?");
    }

    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    public String getManufacturer() {
        return "Divoom";
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return PixooSupport.class;
    }

    @Override
    public InstallHandler findInstallHandler(final Uri uri, final Context context) {
        PixooInstallHandler installHandler = new PixooInstallHandler(uri, context);
        return installHandler.isValid() ? installHandler : null;
    }
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_pixoo;
    }

    @Override
    public boolean supportsFlashing() {
        // To install bitmaps
        return true;
    }

    @Override
    public boolean supportsWeather() {
        return true;
    }

    @Override
    public int getAlarmSlotCount(GBDevice device) {
        return 10;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_lovetoy;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_lovetoy_disabled;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_header_display,
                R.xml.devicesettings_screen_brightness,
                R.xml.devicesettings_header_time,
                R.xml.devicesettings_timeformat,
                R.xml.devicesettings_header_notifications,
                R.xml.devicesettings_send_app_notifications,
                R.xml.devicesettings_header_other,
                R.xml.devicesettings_pixoo,
                R.xml.devicesettings_header_connection,
                R.xml.devicesettings_device_name,
        };
    }
}
