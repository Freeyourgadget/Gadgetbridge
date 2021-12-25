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

public enum IntFormat {
    UINT8(1, false),
    SINT8(1, true),
    UINT16( 2, false),
    SINT16( 2, true),
    UINT32(4, false),
    SINT32(4, true);

    final int bytesCount;
    final boolean isSigned;
    final long max;
    final long min;

    IntFormat(int bytesCount, boolean isSigned) {
        this.bytesCount = bytesCount;
        this.isSigned = isSigned;
        int bitsCount = bytesCount * 8;
        long max;
        if (isSigned) {
            max = (long) Math.pow(2.0, bitsCount - 1) - 1L;
        } else {
            max = (long) (Math.pow(2.0, bitsCount) - 1.0);
        }
        this.max = max;
        long min;
        if (isSigned) {
            min = (long) (-1.0 * Math.pow(2.0, bitsCount - 1));
        } else {
            min = 0L;
        }
        this.min = min;
    }
}
