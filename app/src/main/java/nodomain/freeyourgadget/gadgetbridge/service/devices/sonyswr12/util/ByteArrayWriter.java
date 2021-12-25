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

import java.util.Arrays;

public class ByteArrayWriter {
    public byte[] byteArray;
    private int bytesWritten;

    public ByteArrayWriter() {
        this.bytesWritten = 0;
    }

    private void addIntToValue(long n, IntFormat intFormat) {
        for (int i = 0; i < intFormat.bytesCount; ++i) {
            this.byteArray[this.bytesWritten++] = (byte) (n >> i * 8 & 0xFFL);
        }
    }

    public void appendUint16(int n) {
        this.appendValue(n, IntFormat.UINT16);
    }

    public void appendUint32(long n) {
        this.appendValue(n, IntFormat.UINT32);
    }

    public void appendUint8(int n) {
        this.appendValue(n, IntFormat.UINT8);
    }

    public void appendValue(long lng, IntFormat intFormat) {
        if (intFormat == null) {
            throw new IllegalArgumentException("wrong int format");
        }
        if (lng > intFormat.max || lng < intFormat.min) {
            throw new IllegalArgumentException("wrong value for intFormat. max: " + intFormat.max + " min: " + intFormat.min + " value: " + lng);
        }
        this.increaseByteArray(intFormat.bytesCount);
        long n = lng;
        if (intFormat.isSigned) {
            int n2 = intFormat.bytesCount * 8;
            n = lng;
            if (lng < 0L) {
                n = (1 << n2 - 1) + ((long) ((1 << n2 - 1) - 1) & lng);
            }
        }
        this.addIntToValue(n, intFormat);
    }

    public void increaseByteArray(int n) {
        if (this.byteArray == null) {
            this.byteArray = new byte[n];
            return;
        }
        this.byteArray = Arrays.copyOf(this.byteArray, this.byteArray.length + n);
    }

    public byte[] getByteArray() {
        return this.byteArray.clone();
    }
}
