/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer

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

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareInfo;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huami.HuamiFirmwareType;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;

public class AmazfitBipFirmwareInfo extends HuamiFirmwareInfo {
    // gps detection is totally bogus, just the first 16 bytes
    private static final byte[] GPS_HEADER = new byte[]{
            (byte) 0xcb, 0x51, (byte) 0xc1, 0x30, 0x41, (byte) 0x9e, 0x5e, (byte) 0xd3,
            0x51, 0x35, (byte) 0xdf, 0x66, (byte) 0xed, (byte) 0xd9, 0x5f, (byte) 0xa7
    };
    private static final byte[] GPS_HEADER2 = new byte[]{
            0x10, 0x50, 0x26, 0x76, (byte) 0x8f, 0x4a, (byte) 0xa1, 0x49,
            (byte) 0xa7, 0x26, (byte) 0xd0, (byte) 0xe6, 0x4a, 0x21, (byte) 0x88, (byte) 0xd4
    };
    private static final byte[] GPS_HEADER3 = new byte[]{
            (byte) 0xeb, (byte) 0xfa, (byte) 0xc5, (byte) 0x89, (byte) 0xf0, 0x5c, 0x2e, (byte) 0xcc,
            (byte) 0xfa, (byte) 0xf3, 0x62, (byte) 0xeb, (byte) 0x92, (byte) 0xc6, (byte) 0xa1, (byte) 0xbb
    };

    private static final byte[] GPS_HEADER4 = new byte[]{
            0x0b, 0x61, 0x53, (byte) 0xed, (byte) 0x83, (byte) 0xac, 0x07, 0x21,
            (byte) 0x8c, 0x36, 0x2e, (byte) 0x8c, (byte) 0x9c, 0x08, 0x54, (byte) 0xa6
    };

    // guessed - at least it is the same across versions from 0.0.7.x to 0.0.9.x
    // and different from other devices
    private static final byte[] FW_HEADER = new byte[]{
            0x68, 0x46, 0x70, 0x47, 0x68, 0x46, 0x70, 0x47,
            0x68, 0x46, 0x70, 0x47, 0x68, 0x46, 0x70, 0x47
    };

    // guessed - this is true for 0.1.0.11
    private static final byte[] FW_HEADER_NEW = new byte[]{
            0x60, (byte) 0xeb, 0x03, 0x0c, 0x70, 0x46, 0x31, 0x46,
            0x3a, 0x46, 0x63, 0x46, (byte) 0xbd, (byte) 0xe8, (byte) 0xf0, (byte) 0x81
    };

    private static final int FW_HEADER_OFFSET = 0x9330;

    private static final byte[] GPS_ALMANAC_HEADER = new byte[]{ // probably wrong
            (byte) 0xa0, (byte) 0x80, 0x08, 0x00, (byte) 0x8b, 0x07
    };

    private static final byte[] GPS_CEP_HEADER = new byte[]{ // probably wrong
            0x2a, 0x12, (byte) 0xa0, 0x02
    };

    private static Map<Integer, String> crcToVersion = new HashMap<>();
    static {
        // firmware
        crcToVersion.put(25257, "0.0.8.74");
        crcToVersion.put(57724, "0.0.8.88");
        crcToVersion.put(27668, "0.0.8.96");
        crcToVersion.put(60173, "0.0.8.97");
        crcToVersion.put(3462,  "0.0.8.98");
        crcToVersion.put(55420, "0.0.9.14");
        crcToVersion.put(39465, "0.0.9.26");
        crcToVersion.put(27394, "0.0.9.40");
        crcToVersion.put(24736, "0.0.9.49");
        crcToVersion.put(49555, "0.0.9.59");
        crcToVersion.put(28586, "0.1.0.08");
        crcToVersion.put(26714, "0.1.0.11");
        crcToVersion.put(64160, "0.1.0.17");
        crcToVersion.put(21992, "0.1.0.26");
        crcToVersion.put(43028, "0.1.0.27");
        crcToVersion.put(59462, "0.1.0.33");
        crcToVersion.put(55277, "0.1.0.39");
        crcToVersion.put(47685, "0.1.0.43");
        crcToVersion.put(2839,  "0.1.0.44");
        crcToVersion.put(30229, "0.1.0.45");
        crcToVersion.put(24302, "0.1.0.70");
        crcToVersion.put(1333,  "0.1.0.80");
        crcToVersion.put(12017, "0.1.0.86");

        // resources
        crcToVersion.put(12586, "0.0.8.74");
        crcToVersion.put(34068, "0.0.8.88");
        crcToVersion.put(59839, "0.0.8.96-98");
        crcToVersion.put(50401, "0.0.9.14-26");
        crcToVersion.put(22051, "0.0.9.40");
        crcToVersion.put(46233, "0.0.9.49-0.1.0.11");
        crcToVersion.put(12098, "0.1.0.17");
        crcToVersion.put(28696, "0.1.0.26-0.1.0.27");
        crcToVersion.put(5650,  "0.1.0.33");
        crcToVersion.put(16117, "0.1.0.39-0.1.0.45");
        crcToVersion.put(22506, "0.1.0.66-0.1.0.70");
        crcToVersion.put(42264, "0.1.0.77-0.1.0.80");
        crcToVersion.put(55934, "0.1.0.86-0.1.0.89");

        // gps
        crcToVersion.put(61520, "9367,8f79a91,0,0,");
        crcToVersion.put(8784,  "9565,dfbd8fa,0,0,");
        crcToVersion.put(16716, "9565,dfbd8faf42,0");
        crcToVersion.put(54154, "9567,8b05506,0,0,");

        // font
        crcToVersion.put(61054, "8");
        crcToVersion.put(62291, "9 (Latin)");
    }

    public AmazfitBipFirmwareInfo(byte[] bytes) {
        super(bytes);
    }

    @Override
    protected HuamiFirmwareType determineFirmwareType(byte[] bytes) {
        if (ArrayUtils.startsWith(bytes, RES_HEADER) || ArrayUtils.startsWith(bytes, NEWRES_HEADER)) {
            if (bytes.length > 500000) { // dont know how to distinguish from Cor .res
                return HuamiFirmwareType.INVALID;
            }
            return HuamiFirmwareType.RES;
        }
        if (ArrayUtils.startsWith(bytes, GPS_HEADER) || ArrayUtils.startsWith(bytes, GPS_HEADER2) || ArrayUtils.startsWith(bytes, GPS_HEADER3) || ArrayUtils.startsWith(bytes, GPS_HEADER4)) {
            return HuamiFirmwareType.GPS;
        }
        if (ArrayUtils.startsWith(bytes, GPS_ALMANAC_HEADER)) {
            return HuamiFirmwareType.GPS_ALMANAC;
        }
        if (ArrayUtils.startsWith(bytes, GPS_CEP_HEADER)) {
            return HuamiFirmwareType.GPS_CEP;
        }
        if (ArrayUtils.equals(bytes, FW_HEADER, FW_HEADER_OFFSET) || ArrayUtils.equals(bytes, FW_HEADER_NEW, FW_HEADER_OFFSET)) {
            // TODO: this is certainly not a correct validation, but it works for now
            return HuamiFirmwareType.FIRMWARE;
        }
        if (ArrayUtils.startsWith(bytes, WATCHFACE_HEADER)) {
            return HuamiFirmwareType.WATCHFACE;
        }
        if (ArrayUtils.startsWith(bytes, NEWFT_HEADER)) {
            if (bytes[10] == 0x01) {
                return HuamiFirmwareType.FONT;
            } else if (bytes[10] == 0x02) {
                return HuamiFirmwareType.FONT_LATIN;
            }
        }
        return HuamiFirmwareType.INVALID;
    }

    @Override
    public boolean isGenerallyCompatibleWith(GBDevice device) {
        return isHeaderValid() && device.getType() == DeviceType.AMAZFITBIP;
    }

    @Override
    protected Map<Integer, String> getCrcMap() {
        return crcToVersion;
    }
}
