/*  Copyright (C) 2017-2020 Andreas Shimokawa

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils;
import nodomain.freeyourgadget.gadgetbridge.util.CheckSums;


public abstract class HuamiFirmwareInfo {

    protected static final byte[] RES_HEADER = new byte[]{ // HMRES resources file (*.res)
            0x48, 0x4d, 0x52, 0x45, 0x53
    };

    protected static final byte[] NEWRES_HEADER = new byte[]{ // NERES resources file (*.res)
            0x4e, 0x45, 0x52, 0x45, 0x53
    };

    protected static final byte[] WATCHFACE_HEADER = new byte[]{
            0x48, 0x4d, 0x44, 0x49, 0x41, 0x4c
    };

    protected static final byte[] FT_HEADER = new byte[]{ // HMZK font file (*.ft, *.ft.xx)
            0x48, 0x4d, 0x5a, 0x4b
    };

    protected static final byte[] NEWFT_HEADER = new byte[]{ // NEZK font file (*.ft, *.ft.xx)
            0x4e, 0x45, 0x5a, 0x4b
    };

    public static final byte[] GPS_ALMANAC_HEADER = new byte[]{ // probably wrong
            (byte) 0xa0, (byte) 0x80, 0x08, 0x00, (byte) 0x8b
    };

    public static final byte[] GPS_CEP_HEADER = new byte[]{ // probably wrong
            0x2a, 0x12, (byte) 0xa0, 0x02
    };

    // gps detection is totally bogus, just the first 16 bytes
    protected static final byte[][] GPS_HEADERS = {
            new byte[]{
                    (byte) 0xcb, 0x51, (byte) 0xc1, 0x30, 0x41, (byte) 0x9e, 0x5e, (byte) 0xd3,
                    0x51, 0x35, (byte) 0xdf, 0x66, (byte) 0xed, (byte) 0xd9, 0x5f, (byte) 0xa7
            },
            new byte[]{
                    0x10, 0x50, 0x26, 0x76, (byte) 0x8f, 0x4a, (byte) 0xa1, 0x49,
                    (byte) 0xa7, 0x26, (byte) 0xd0, (byte) 0xe6, 0x4a, 0x21, (byte) 0x88, (byte) 0xd4
            },
            new byte[]{
                    (byte) 0xeb, (byte) 0xfa, (byte) 0xc5, (byte) 0x89, (byte) 0xf0, 0x5c, 0x2e, (byte) 0xcc,
                    (byte) 0xfa, (byte) 0xf3, 0x62, (byte) 0xeb, (byte) 0x92, (byte) 0xc6, (byte) 0xa1, (byte) 0xbb
            },
            new byte[]{
                    0x0b, 0x61, 0x53, (byte) 0xed, (byte) 0x83, (byte) 0xac, 0x07, 0x21,
                    (byte) 0x8c, 0x36, 0x2e, (byte) 0x8c, (byte) 0x9c, 0x08, 0x54, (byte) 0xa6
            },
            new byte[]{
                    (byte) 0xec, 0x51, 0x73, 0x22, 0x60, 0x02, 0x14, (byte) 0xb7,
                    (byte) 0xb5, (byte) 0xea, 0x4b, 0x22, 0x5d, 0x23, (byte) 0xe5, 0x4f
            },
            new byte[]{
                    0x73, 0x75, 0x68, (byte) 0xd0, 0x70, 0x73, (byte) 0xbb, 0x5a,
                    0x3e, (byte) 0xc3, (byte) 0xd3, 0x09, (byte) 0x9e, 0x1d, (byte) 0xd3, (byte) 0xc9
            },
            new byte[]{
                    0x00, 0x00, 0x00, 0x01, 0x00, 0x09, 0x6F, (byte) 0xD0,
                    0x00, 0x01, 0x00, 0x02, 0x3D, (byte) 0xE0, 0x00, 0x69
            }
    };

    protected static final int FONT_TYPE_OFFSET = 0x9;
    protected static final int COMPRESSED_RES_HEADER_OFFSET = 0x9;
    protected static final int COMPRESSED_RES_HEADER_OFFSET_NEW = 0xd;

    private HuamiFirmwareType firmwareType;

    public String toVersion(int crc16) {
        String version = getCrcMap().get(crc16);
        if (version == null) {
            switch (firmwareType) {
                case FIRMWARE:
                    version = searchFirmwareVersion(bytes);
                    break;
                case RES:
                    version = "RES " + bytes[5];
                    break;
                case RES_COMPRESSED:
                    byte versionByte;
                    // there are two possible locations of the version for compressed res, probe the format in a dirty way :P
                    if (bytes[COMPRESSED_RES_HEADER_OFFSET + 2] == 0x52 &&
                            bytes[COMPRESSED_RES_HEADER_OFFSET + 3] == 0x45 &&
                            bytes[COMPRESSED_RES_HEADER_OFFSET + 4] == 0x53) {
                        versionByte = bytes[14];
                    } else {
                        versionByte = bytes[18];
                    }
                    version = "RES " + (versionByte & 0xff);
                    break;
                case FONT:
                    version = "FONT " + bytes[4];
                    break;
                case FONT_LATIN:
                    version = "FONT LATIN " + bytes[4];
                    break;
            }
        }
        if (version == null) {
            switch (firmwareType) {
                case FIRMWARE:
                    version = "(unknown)";
                    break;
                case FONT:
                case FONT_LATIN:
                    version = "(unknown font)";
                    break;
                case GPS:
                    version = "(unknown GPS)";
                    break;
                case GPS_CEP:
                    version = "(unknown CEP)";
                    break;
                case GPS_ALMANAC:
                    version = "(unknown ALM)";
                    break;
                case WATCHFACE:
                    version = "(unknown watchface)";
                    break;
            }
        }
        return version;
    }

    public int[] getWhitelistedVersions() {
        return ArrayUtils.toIntArray(getCrcMap().keySet());
    }

    private final int crc16;
    private final int crc32;

    private byte[] bytes;

    public HuamiFirmwareInfo(byte[] bytes) {
        this.bytes = bytes;
        crc16 = CheckSums.getCRC16(bytes);
        crc32 = CheckSums.getCRC32(bytes);
        firmwareType = determineFirmwareType(bytes);
    }

    public abstract boolean isGenerallyCompatibleWith(GBDevice device);

    public boolean isHeaderValid() {
        return getFirmwareType() != HuamiFirmwareType.INVALID;
    }

    public void checkValid() throws IllegalArgumentException {
    }

    /**
     * @return the size of the firmware in number of bytes.
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
    public int getCrc32() {
        return crc32;
    }

    public int getFirmwareVersion() {
        return getCrc16(); // HACK until we know how to determine the version from the fw bytes
    }

    public HuamiFirmwareType getFirmwareType() {
        return firmwareType;
    }

    protected abstract Map<Integer, String> getCrcMap();

    protected abstract HuamiFirmwareType determineFirmwareType(byte[] bytes);

    protected String searchFirmwareVersion(byte[] fwbytes) {
        ByteBuffer buf = ByteBuffer.wrap(fwbytes);
        buf.order(ByteOrder.BIG_ENDIAN);
        while (buf.remaining() > 3) {
            int word = buf.getInt();
            if (word == 0x5625642e) {
                word = buf.getInt();
                if (word == 0x25642e25) {
                    word = buf.getInt();
                    if (word == 0x642e2564) {
                        word = buf.getInt();
                        if (word == 0x00000000) {
                            byte[] version = new byte[8];
                            buf.get(version);
                            return new String(version);
                        }
                    }
                }
            }
        }
        return null;
    }

    protected boolean searchString32BitAligned(byte[] fwbytes, String findString) {
        ByteBuffer stringBuf = ByteBuffer.wrap((findString + "\0").getBytes());
        stringBuf.order(ByteOrder.BIG_ENDIAN);
        int[] findArray = new int[stringBuf.remaining() / 4];
        for (int i = 0; i < findArray.length; i++) {
            findArray[i] = stringBuf.getInt();
        }

        ByteBuffer buf = ByteBuffer.wrap(fwbytes);
        buf.order(ByteOrder.BIG_ENDIAN);
        while (buf.remaining() > 3) {
            int arrayPos = 0;
            while (arrayPos < findArray.length && buf.remaining() > 3 && (buf.getInt() == findArray[arrayPos])) {
                arrayPos++;
            }
            if (arrayPos == findArray.length) {
                return true;
            }
        }
        return false;
    }
}
