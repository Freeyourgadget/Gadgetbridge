/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.status;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;

public class DeviceInfo {
    public final String firmwareName;
    public final String serialNo;

    public DeviceInfo(byte[] packet) {
        ByteBuffer buf = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        byte[] fwNameStr = new byte[8];
        buf.get(fwNameStr);
        serialNo = Integer.toString(buf.getInt());
        firmwareName = new String(fwNameStr, StandardCharsets.UTF_8);
    }

    public GBDeviceEventVersionInfo toDeviceEvent() {
        final GBDeviceEventVersionInfo gbDeviceEventVersionInfo = new GBDeviceEventVersionInfo();
        gbDeviceEventVersionInfo.fwVersion = firmwareName;
        gbDeviceEventVersionInfo.fwVersion2 = serialNo;
        return gbDeviceEventVersionInfo;
    }
}
