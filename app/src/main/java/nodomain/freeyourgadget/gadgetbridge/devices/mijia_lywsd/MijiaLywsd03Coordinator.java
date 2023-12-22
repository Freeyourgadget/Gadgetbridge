/*  Copyright (C) 2016-2023 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class MijiaLywsd03Coordinator extends AbstractMijiaLywsdCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("LYWSD03MMC");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_mijia_lywsd03;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_thermometer;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_thermometer_disabled;
    }

    @Override
    public boolean supportsSetTime() {
        return false;
    }
}
