/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;

public class Mi2FirmwareInfo {
    private static final byte[] FW_HEADER = new byte[]{
            (byte) 0xa3,
            (byte) 0x68,
            (byte) 0x04,
            (byte) 0x3b,
            (byte) 0x02,
            (byte) 0xdb,
            (byte) 0xc8,
            (byte) 0x58,
            (byte) 0xd0,
            (byte) 0x50,
            (byte) 0xfa,
            (byte) 0xe7,
            (byte) 0x0c,
            (byte) 0x34,
            (byte) 0xf3,
            (byte) 0xe7,
    };

    private static final int FW_HEADER_OFFSET = 0x150;

    private static final byte[] FT_HEADER = new byte[] { // HMZK font file (*.ft, *.ft.xx)
            0x48,
            0x4d,
            0x5a,
            0x4b
    };

    private static Map<Integer,String> crcToVersion = new HashMap<>();
    static {
        // firmware
        crcToVersion.put(41899, "1.0.0.39");
        crcToVersion.put(49197, "1.0.0.53");
        crcToVersion.put(32450, "1.0.1.28");
        crcToVersion.put(51770, "1.0.1.34");
        crcToVersion.put(3929, "1.0.1.39");

        // fonts
        crcToVersion.put(45624, "Font");
        crcToVersion.put(6377, "Font (En)");
    }

    private FirmwareType firmwareType = FirmwareType.FIRMWARE;

    public static String toVersion(int crc16) {
        return crcToVersion.get(crc16);
    }

    public static int[] getWhitelistedVersions() {
        return ArrayUtils.toIntArray(crcToVersion.keySet());
    }

    private final int crc16;

    private byte[] bytes;
    private String firmwareVersion;

    public Mi2FirmwareInfo(byte[] bytes) {
        this.bytes = bytes;
        crc16 = CheckSums.getCRC16(bytes);
        firmwareVersion = crcToVersion.get(crc16);
        firmwareType = determineFirmwareType(bytes);
    }

    private FirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, FT_HEADER)) {
            return FirmwareType.FONT;
        }
        if (ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET)) {
            // TODO: this is certainly not a correct validation, but it works for now
            return FirmwareType.FIRMWARE;
        }
        return FirmwareType.INVALID;
    }

    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.MIBAND2;
    }

    public boolean isHeaderValid() {
        return getFirmwareType() != FirmwareType.INVALID;
    }

    public void checkValid() throws IllegalArgumentException {
    }

    /**
     * Returns the size of the firmware in number of bytes.
     * @return
     */
    public int getSize() {
        return bytes.length;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getCrc16() {
        return crc16;
    }

    public int getFirmwareVersion() {
        return getCrc16(); // HACK until we know how to determine the version from the fw bytes
    }

    public FirmwareType getFirmwareType() {
        return firmwareType;
    }
}
