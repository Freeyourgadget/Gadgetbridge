/*  Copyright (C) 2021-2024 Daniel Dakhno, José Rebelo, Petr Vaněk

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

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class SonyWISP600NCoordinator extends SonyHeadphonesCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile(".*WI-SP600N.*");
    }
    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_sony_wi_sp600n;
    }

    @Override
    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Arrays.asList(
                SonyHeadphonesCapabilities.BatterySingle,
                SonyHeadphonesCapabilities.AmbientSoundControl,
                SonyHeadphonesCapabilities.WindNoiseReduction,
                SonyHeadphonesCapabilities.EqualizerWithCustomBands,
                SonyHeadphonesCapabilities.SoundPosition,
                SonyHeadphonesCapabilities.SurroundMode,
                SonyHeadphonesCapabilities.PowerOffFromPhone,
                SonyHeadphonesCapabilities.AutomaticPowerOffByTime,
                SonyHeadphonesCapabilities.VoiceNotifications,
                SonyHeadphonesCapabilities.Volume
        );
    }
}
