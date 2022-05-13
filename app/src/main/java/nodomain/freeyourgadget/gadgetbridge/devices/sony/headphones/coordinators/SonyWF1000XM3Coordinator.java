/*  Copyright (C) 2022 Ngô Minh Quang

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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCapabilities;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class SonyWF1000XM3Coordinator extends SonyHeadphonesCoordinator {

    @NonNull
    @Override
    public DeviceType getSupportedType(GBDeviceCandidate candidate) {
        if (candidate.getName().contains("WF-1000XM3")) {
            return DeviceType.SONY_WF_1000XM3;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SONY_WF_1000XM3;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        final BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        final BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        final BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);

        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public List<SonyHeadphonesCapabilities> getCapabilities() {
        return Arrays.asList(
                SonyHeadphonesCapabilities.BatteryDual,
                SonyHeadphonesCapabilities.BatteryCase,
                SonyHeadphonesCapabilities.PowerOffFromPhone,
                SonyHeadphonesCapabilities.AmbientSoundControl,
                SonyHeadphonesCapabilities.WindNoiseReduction,
                SonyHeadphonesCapabilities.Equalizer,
                SonyHeadphonesCapabilities.AudioUpsampling,
                SonyHeadphonesCapabilities.ButtonModesLeftRight,
                SonyHeadphonesCapabilities.PauseWhenTakenOff,
                SonyHeadphonesCapabilities.AutomaticPowerOffWhenTakenOff,
                SonyHeadphonesCapabilities.VoiceNotifications
        );
    }
}
