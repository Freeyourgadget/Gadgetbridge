/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo, Petr Kadlec, protomors, Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;

public class BohemicSmartBraceletDeviceCoordinator extends LefunDeviceCoordinator {
    public boolean supports(GBDeviceCandidate candidate) {
        // Since the Lefun coordinator overrides supports, we also need to
        return ".bohemic".equals(candidate.getName());
    }

    @Override
    public int[] getSupportedDeviceSpecificSettings(GBDevice device) {
        return new int[]{
                R.xml.devicesettings_liftwrist_display_noshed,
                R.xml.devicesettings_timeformat,
                // R.xml.devicesettings_antilost, not supported
                R.xml.devicesettings_inactivity,
                R.xml.devicesettings_hydration_reminder,
                R.xml.devicesettings_lefun_interface_language,
                R.xml.devicesettings_transliteration
        };
    }

    @Override
    public boolean supportsRealtimeData() {
        return false;  // not supported
    }


    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_bohemic_smart_bracelet;
    }


    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_h30_h10;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_h30_h10_disabled;
    }
}
