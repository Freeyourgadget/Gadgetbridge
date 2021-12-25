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

public class UIntBitWriter {
    private long value;
    private long offset;

    public UIntBitWriter(int offset) {
        this.value = 0L;
        this.offset = offset;
    }

    public void append(int offset, int value) {
        if (value < 0 || value > (1 << offset) - 1) {
            throw new IllegalArgumentException("value is out of range: " + value);
        }
        this.offset -= offset;
        if (this.offset < 0L) {
            throw new IllegalArgumentException("Write offset out of range");
        }
        this.value |= (long) value << (int) this.offset;
    }

    public void appendBoolean(boolean b) {
        if (b) {
            this.append(1, 1);
            return;
        }
        this.append(1, 0);
    }

    public long getValue() {
        if (this.offset != 0L) {
            throw new IllegalStateException("value is not complete yet: " + this.offset);
        }
        return this.value;
    }
}