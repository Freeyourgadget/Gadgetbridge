/*  Copyright (C) 2024 Damien Gaignon, Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.devices.huawei.huaweiwatchgt3;

import java.util.regex.Pattern;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiBRCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiConstants;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class HuaweiWatchGT3Coordinator extends HuaweiBRCoordinator {
    @Override
    public DeviceType getDeviceType() {
        return DeviceType.HUAWEIWATCHGT3;
    }

    @Override
    protected Pattern getSupportedDeviceName() {
        return Pattern.compile("(" + HuaweiConstants.HU_WATCHGT3_NAME + "|" + HuaweiConstants.HU_WATCHGT3PRO_NAME + "|" + HuaweiConstants.HU_WATCHGT3SE_NAME + ").*", Pattern.CASE_INSENSITIVE);
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_huawei_watchgt3;
    }

    @Override
    public boolean supportsUnicodeEmojis() {
        // HarmonyOS watch
        return true;
    }
}
