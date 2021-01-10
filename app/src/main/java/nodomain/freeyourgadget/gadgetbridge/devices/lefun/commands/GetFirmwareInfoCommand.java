/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class GetFirmwareInfoCommand extends BaseCommand {
    private short supportCode;
    private short devTypeReserveCode;
    private String typeCode;
    private short hardwareVersion;
    private short softwareVersion;
    private String vendorCode;

    public short getSupportCode() {
        return supportCode;
    }

    public short getDevTypeReserveCode() {
        return devTypeReserveCode;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public short getHardwareVersion() {
        return hardwareVersion;
    }

    public short getSoftwareVersion() {
        return softwareVersion;
    }

    public String getVendorCode() {
        return vendorCode;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_FIRMWARE_INFO, 16);

        supportCode = (short) (params.get() | (params.get() << 8));
        devTypeReserveCode = params.getShort();
        byte[] typeCodeBytes = new byte[4];
        params.get(typeCodeBytes);
        typeCode = new String(typeCodeBytes, StandardCharsets.US_ASCII);
        hardwareVersion = params.getShort();
        softwareVersion = params.getShort();
        byte[] vendorCodeBytes = new byte[4];
        params.get(vendorCodeBytes);
        vendorCode = new String(vendorCodeBytes, StandardCharsets.US_ASCII);
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        return LefunConstants.CMD_FIRMWARE_INFO;
    }
}
