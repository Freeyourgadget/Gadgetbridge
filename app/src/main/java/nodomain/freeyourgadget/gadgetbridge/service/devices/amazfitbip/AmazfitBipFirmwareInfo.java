/*  Copyright (C) 2017 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.amazfitbip;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.FirmwareType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband2.Mi2FirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitBipFirmwareInfo extends Mi2FirmwareInfo {
    // total crap maybe
    private static final byte[] GPS_HEADER = new byte[]{
            (byte) 0xcb, 0x51, (byte) 0xc1, 0x30, 0x41, (byte) 0x9e, 0x5e, (byte) 0xd3,
            0x51, 0x35, (byte) 0xdf, 0x66, (byte) 0xed, (byte) 0xd9, 0x5f, (byte) 0xa7
    };

    // guessed - at least it is the same accross current versions and different from other devices
    private static final byte[] FW_HEADER = new byte[]{
            0x3f, 0x34, 0x00, 0x20, 0x27, 0x35, 0x00, 0x20,
            0x0d, 0x31, 0x03, 0x00, 0x15, 0x35, 0x00, 0x20
    };

    private static final int FW_HEADER_OFFSET = 0x40;

    private static final byte[] RES_HEADER = new byte[]{ // HMRES resources file (*.res)
            0x48, 0x4d, 0x52, 0x45, 0x53
    };


    static {
        // firmware
        crcToVersion.put(25257, "0.0.8.74");

        // resources
        crcToVersion.put(12586, "0.0.8.74 (RES)");

        // gps
    }

    public AmazfitBipFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected FirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, RES_HEADER)) {
            return FirmwareType.RES;
        }
        if (ArrayUtils.startsWith(bytes, GPS_HEADER)) {
            return FirmwareType.GPS;
        }
        if (ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET)) {
            // TODO: this is certainly not a correct validation, but it works for now
            return FirmwareType.FIRMWARE;
        }
        return FirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITBIP;
    }

}
