/*  Copyright (C) 2023-2024 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.nio.ByteBuffer;

public final class ChecksumCalculator {
    private static final int[] CONSTANTS = {
            0x0000, 0xCC01, 0xD801, 0x1400, 0xF001, 0x3C00, 0x2800, 0xE401,
            0xA001, 0x6C00,0x7800, 0xB401, 0x5000, 0x9C01, 0x8801, 0x4400
    };

    private ChecksumCalculator() {
    }

    public static int computeCrc(byte[] data, int offset, int length) {
        return computeCrc(0, data, offset, length);
    }

    public static int computeCrc(ByteBuffer byteBuffer, int offset, int length) {
        byteBuffer.rewind();
        byte[] data = new byte[length];
        byteBuffer.get(data);
        return computeCrc(0, data, offset, length);
    }

    public static int computeCrc(int initialCrc, byte[] data, int offset, int length) {
        int crc = initialCrc;
        for (int i = offset; i < offset + length; ++i) {
            int b = data[i];
            crc = (((crc >> 4) & 4095) ^ CONSTANTS[crc & 15]) ^ CONSTANTS[b & 15];
            crc = (((crc >> 4) & 4095) ^ CONSTANTS[crc & 15]) ^ CONSTANTS[(b >> 4) & 15];
        }
        return crc;
    }
}
