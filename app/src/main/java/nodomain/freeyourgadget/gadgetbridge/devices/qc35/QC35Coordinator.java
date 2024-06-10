/*  Copyright (C) 2021-2024 Damien Gaignon, Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.qc35;

import android.app.Activity;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qc35.QC35BaseSupport;

public class QC35Coordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    protected void deleteDevice(@NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session) throws GBException {

    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Bose QC 35.*");
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_qc35
        };
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return QC35BaseSupport.class;
    }

    @Override
    public String getManufacturer() {
        return "Bose";
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_bose_qc35;
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        return new BatteryConfig[]{
                new BatteryConfig(
                        0,
                        GBDevice.BATTERY_ICON_DEFAULT,
                        GBDevice.BATTERY_LABEL_DEFAULT,
                        25,
                        100
                )
        };
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_headphones;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_headphones_disabled;
    }
}
