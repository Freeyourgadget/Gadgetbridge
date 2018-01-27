/*  Copyright (C) 2017 Andreas Shimokawa, Carsten Pfeiffer

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

public enum HuamiFirmwareType {
    FIRMWARE((byte) 0),
    FONT((byte) 1),
    RES((byte) 2),
    RES_COMPRESSED((byte)130),
    GPS((byte) 3),
    GPS_CEP((byte) 4),
    GPS_ALMANAC((byte)5),
    WATCHFACE((byte)8),
    FONT_LATIN((byte)11),
    INVALID(Byte.MIN_VALUE);

    private final byte value;

    HuamiFirmwareType(byte value) {
        this.value = value;
    }

    public byte getValue() {
        return value;
    }
}
