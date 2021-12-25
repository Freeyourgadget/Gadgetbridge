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

public class UIntBitReader {
    private final long value;
    private int offset;

    public UIntBitReader(long value, int offset) {
        this.value = value;
        this.offset = offset;
    }

    public int read(int offset) {
        this.offset -= offset;
        if (this.offset < 0) {
            throw new IllegalArgumentException("Read out of range");
        }
        return (int) ((long) ((1 << offset) - 1) & this.value >>> this.offset);
    }

    public boolean readBoolean() {
        boolean b = true;
        if (this.read(1) == 0) {
            b = false;
        }
        return b;
    }
}
