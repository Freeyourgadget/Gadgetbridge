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
package nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.coordinators;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.headphones.SonyHeadphonesCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryConfig;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class SonyWFSP800NCoordinator extends SonyHeadphonesCoordinator {
    @NonNull
    @Override
    public DeviceType getSupportedType(final GBDeviceCandidate candidate) {
        if (candidate.getName().contains("WF-SP800N")) {
            return DeviceType.SONY_WF_SP800N;
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.SONY_WF_SP800N;
    }

    @Override
    public int getBatteryCount() {
        return 3;
    }

    @Override
    public boolean supportsPowerOff() {
        return true;
    }

    @Override
    public BatteryConfig[] getBatteryConfig() {
        final BatteryConfig battery1 = new BatteryConfig(0, R.drawable.ic_tws_case, R.string.battery_case);
        final BatteryConfig battery2 = new BatteryConfig(1, R.drawable.ic_galaxy_buds_l, R.string.left_earbud);
        final BatteryConfig battery3 = new BatteryConfig(2, R.drawable.ic_galaxy_buds_r, R.string.right_earbud);

        return new BatteryConfig[]{battery1, battery2, battery3};
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(final GBDevice device) {
        return new int[]{
                R.xml.devicesettings_sony_headphones_ambient_sound_control,
                R.xml.devicesettings_header_other,
                R.xml.devicesettings_sony_headphones_equalizer,
                R.xml.devicesettings_header_system,
                R.xml.devicesettings_sony_headphones_button_modes_left_right,
                R.xml.devicesettings_sony_headphones_pause_when_taken_off,
                R.xml.devicesettings_sony_wf_sp800n,
                R.xml.devicesettings_sony_headphones_notifications_voice_guide
        };
    }
}
