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

public class SonyWIC100Coordinator extends SonyHeadphonesCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(".*WI-C100.*");
    }

    @Override
    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Arrays.asList(
                SonyHeadphonesCapabilities.BatterySingle,
                SonyHeadphonesCapabilities.EqualizerSimple,
                SonyHeadphonesCapabilities.EqualizerWithCustomBands,
                SonyHeadphonesCapabilities.AudioUpsampling,
                SonyHeadphonesCapabilities.VoiceNotifications,
                SonyHeadphonesCapabilities.PowerOffFromPhone
        );
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wi_c100;
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
