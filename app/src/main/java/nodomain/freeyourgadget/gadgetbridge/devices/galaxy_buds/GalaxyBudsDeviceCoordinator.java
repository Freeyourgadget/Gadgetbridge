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

import androidx.annotation.NonNull;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class GalaxyBudsDeviceCoordinator extends GalaxyBudsGenericCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("Galaxy Buds \\(.*");
    }

    @Override
    public int getBatteryCount() {
        return 2;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);
        return new BatteryConfig[]{battery1, battery2};
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_galaxybuds;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_galaxy_buds,
        };
    }
}
