/*  Copyright (C) 2021-2024 Daniel Dakhno, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.devices.galaxy_buds;

import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettings;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSpecificSettingsScreen;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;

public class GalaxyBudsLiveDeviceCoordinator extends GalaxyBudsGenericCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Galaxy Buds Live \\(.*");
    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_live_l, R.string.left_earbud);
        BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_galaxy_buds_live_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public DeviceSpecificSettings getDeviceSpecificSettings(final GBDevice device) {
        final DeviceSpecificSettings deviceSpecificSettings = new DeviceSpecificSettings();
        final List<Integer> audio = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.AUDIO);
        audio.add(R.xml.devicesettings_galaxy_buds_live);
        final List<Integer> callsAndNotif = deviceSpecificSettings.addRootScreen(DeviceSpecificSettingsScreen.CALLS_AND_NOTIFICATIONS);
        callsAndNotif.add(R.xml.devicesettings_headphones);
        return deviceSpecificSettings;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_galaxybuds_live;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_galaxy_buds_live;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_galaxy_buds_live_disabled;
    }
}
