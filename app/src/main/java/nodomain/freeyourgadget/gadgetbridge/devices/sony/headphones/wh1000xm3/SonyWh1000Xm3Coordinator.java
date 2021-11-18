/*  Copyright (C) 2021 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.wh1000xm3;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class SonyWh1000Xm3Coordinator extends SonyHeadphonesCoordinator {
    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        if (candidate.getName().contains("WH-1000XM3")) {
            return DeviceType.SONY_WH_1000XM3;
        }
        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SONY_WH_1000XM3;
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        return new int[]{
                R.xml.devicesettings_sony_warning_wh1000xm3,
                R.xml.devicesettings_sony_headphones_ambient_sound_control,
                R.xml.devicesettings_sony_headphones_equalizer,
                R.xml.devicesettings_sony_headphones_other,
                R.xml.devicesettings_sony_wh_1000xm3
        };
    }
}
