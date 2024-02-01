/*  Copyright (C) 2022-2024 Daniel Dakhno, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband7;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.zeppos.ZeppOsCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;

public class MiBand7Coordinator extends ZeppOsCoordinator {
    @Override
    public String getDeviceBluetoothName() {
        return HuamiConst.XIAOMI_SMART_BAND7_NAME;
    }

    @Override
    public Set<Integer> getDeviceSources() {
        return new HashSet<>(Arrays.asList(260, 262, 263, 264, 265));
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return new HashMap<Integer, String>() {{
            // firmware
            put(26036, "1.20.3.1");
            put(55449, "1.27.0.4");
            put(14502, "2.0.0.2");
            put(25658, "2.1.0.1");
        }};
    }

    @Override
    public boolean supports(final GBDeviceCandidate candidate) {
        final String name = candidate.getName();
        return name.startsWith(HuamiConst.XIAOMI_SMART_BAND7_NAME) && !name.contains("Pro");
    }

    @Override
    public boolean supportsAgpsUpdates() {
        return false;
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return false;
    }

    @Override
    public int getDeviceNameResource() {
        return R.string.devicetype_miband7;
    }

    @Override
    public int getDefaultIconResource() {
        return R.drawable.ic_device_miband6;
    }

    @Override
    public int getDisabledIconResource() {
        return R.drawable.ic_device_miband6_disabled;
    }
}
