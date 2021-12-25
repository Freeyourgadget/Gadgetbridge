/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util;

public class ByteArrayReader {
    public final byte[] byteArray;
    public int bytesRead;

    public ByteArrayReader(byte[] array) {
        this.bytesRead = 0;
        if (array == null || array.length <= 0) {
            throw new IllegalArgumentException("wrong byte array");
        }
        this.byteArray = array.clone();
    }

    public int getBytesLeft() {
        return this.byteArray.length - this.bytesRead;
    }

    public long readInt(IntFormat intFormat) {
        if (intFormat == null) {
            throw new IllegalArgumentException("wrong intFormat");
        }
        int i = 0;
        long n = 0L;
        try {
            while (i < intFormat.bytesCount) {
                long n2 = this.byteArray[this.bytesRead++] & 0xFF;
                int n3 = i + 1;
                n += n2 << i * 8;
                i = n3;
            }
            long n4 = n;
            if (intFormat.isSigned) {
                int n5 = intFormat.bytesCount * 8;
                n4 = n;
                if (((long) (1 << n5 - 1) & n) != 0x0L) {
                    n4 = ((1 << n5 - 1) - (n & (long) ((1 << n5 - 1) - 1))) * -1L;
                }
            }
            return n4;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new RuntimeException("reading outside of byte array", ex.getCause());
        }
    }

    public int readUint16() {
        return (int) this.readInt(IntFormat.UINT16);
    }

    public int readUint8() {
        return (int) this.readInt(IntFormat.UINT8);
    }
}
