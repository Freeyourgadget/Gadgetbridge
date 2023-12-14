/*  Copyright (C) 2016-2023 Petr Vaněk, José Rebelo

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

package nodomain.freeyourgadget.gadgetbridge.devices.fitpro.colacao;

import androidx.annotation.DrawableRes;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProDeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ColaCao23Coordinator extends FitProDeviceCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("ColaCao23");
    }

    @Override
    public boolean supportsHeartRateMeasurement(final GBDevice device) {
        // HR requests crash the device
        return false;
    }

    @Override
    @DrawableRes
    public int getDefaultIconResource() {
        return R.drawable.ic_device_amazfit_bip;
    }

    @Override
    @DrawableRes
    public int getDisabledIconResource() {
        return R.drawable.ic_device_amazfit_bip_disabled;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_colacao23;
    }
}
