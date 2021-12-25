/*  Copyright (C) 2020-2021 Andreas Böhler

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
package nodomain.freeyourgadget.gadgetbridge.util;

import androidx.annotation.IntRange;

public class BcdUtil {
    public static byte toBcd8(@IntRange(from = 0, to = 99) int value) {
        int high = (value / 10) << 4;
        int low = value % 10;
        return (byte) (high | low);
    }

    public static int fromBcd8(byte value) {
        int high = ((value & 0xF0) >> 4) * 10;
        int low = value & 0x0F;
        return high + low;
    }
}
