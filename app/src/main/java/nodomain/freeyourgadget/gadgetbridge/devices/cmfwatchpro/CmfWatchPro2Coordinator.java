/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.cmfwatchpro;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;

public class CmfWatchPro2Coordinator extends CmfWatchProCoordinator {
    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("^CMF Watch Pro 2(-[A-Z0-9]{4})$");
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_nothing_cmf_watch_pro_2;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_watchxplus;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_watchxplus_disabled;
    }

    @Override
    public int getBondingStyle() {
        // We can negotiate auth key - #3982
        return BONDING_STYLE_BOND;
    }

    @Override
    public boolean supportsSunriseSunset() {
        return true;
    }
}
