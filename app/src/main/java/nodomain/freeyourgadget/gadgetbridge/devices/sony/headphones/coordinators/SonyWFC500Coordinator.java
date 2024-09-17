/*  Copyright (C) 2024 Zahnstocher

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;

public class SonyWFC500Coordinator extends SonyHeadphonesCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(".*WF-C500.*");
    }

    @Override
    public BatteryConfig[] getBatteryConfig(final GBDevice device) {
        final BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        final BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);

        return new BatteryConfig[]{battery1, battery2};
    }

    @Override
    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Arrays.asList(
                SonyHeadphonesCapabilities.BatteryDual_2,
                SonyHeadphonesCapabilities.EqualizerSimple,
                SonyHeadphonesCapabilities.EqualizerWithCustomBands,
                SonyHeadphonesCapabilities.AudioUpsampling,
                SonyHeadphonesCapabilities.PowerOffFromPhone
        );
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wf_c500;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_galaxy_buds;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_galaxy_buds_disabled;
    }
}
