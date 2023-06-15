/*  Copyright (C) 2022 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.huami.miband7;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.Huami2021Coordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiConst;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDeviceCandidate;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

public class MiBand7Coordinator extends Huami2021Coordinator {
    private static final Logger LOG = LoggerFactory.getLogger(MiBand7Coordinator.class);

    @NonNull
    @Override
    public DeviceType getSupportedType(final GBDeviceCandidate candidate) {
        try {
            final BluetoothDevice device = candidate.getDevice();
            final String name = device.getName();
            if (name != null && name.startsWith(HuamiConst.XIAOMI_SMART_BAND7_NAME)) {
                return DeviceType.MIBAND7;
            }
        } catch (final Exception e) {
            LOG.error("unable to check device support", e);
        }

        return DeviceType.UNKNOWN;
    }

    @Override
    public DeviceType getDeviceType() {
        return DeviceType.MIBAND7;
    }

    @Override
    public String deviceName() {
        return HuamiConst.XIAOMI_SMART_BAND7_NAME;
    }

    @Override
    public Set<Integer> deviceSources() {
        return new HashSet<>(Arrays.asList(260, 262, 263, 264, 265));
    }

    @Override
    public Map<Integer, String> getFirmwareCrcMap() {
        return new HashMap<Integer, String>() {{
            // firmware
            put(26036, "1.20.3.1");
            put(55449, "1.27.0.4");
            put(14502, "2.0.0.2");
        }};
    }

    @Override
    public boolean supportsScreenshots() {
        return false;
    }

    @Override
    public boolean supportsBluetoothPhoneCalls(final GBDevice device) {
        return false;
    }
}
