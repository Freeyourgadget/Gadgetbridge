/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.devices.moondrop;

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractBLClassicDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.moondrop.MoondropSpaceTravelDeviceSupport;

public class MoondropSpaceTravelCoordinator extends AbstractBLClassicDeviceCoordinator {
    @Override
    public String getManufacturer() {
        return "Moondrop";
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Moondrop Space Travel");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_moondrop_space_travel;
    }

    @Override
    public int getBatteryCount() {
        return 0;
    }

    @Override
    protected void deleteDevice(
            @NonNull GBDevice gbDevice, @NonNull Device device, @NonNull DaoSession session)
            throws GBException {}

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings settings = new DeviceSpecificSettings();

        settings.addRootScreen(R.xml.devicesettings_moondrop_space_travel_equalizer);
        settings.addRootScreen(DeviceSpecificSettingsScreen.TOUCH_OPTIONS);
        settings.addSubScreen(
                DeviceSpecificSettingsScreen.TOUCH_OPTIONS,
                R.xml.devicesettings_moondrop_space_travel_touch);
        settings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        settings.addSubScreen(
                DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS,
                R.xml.devicesettings_headphones);

        return settings;
    }

    @NonNull
    @Override
    public Class<? extends DeviceSupport> getDeviceSupportClass() {
        return MoondropSpaceTravelDeviceSupport.class;
    }
}
