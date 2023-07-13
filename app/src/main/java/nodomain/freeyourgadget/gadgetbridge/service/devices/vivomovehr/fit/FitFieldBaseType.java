/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import android.util.SparseArray;

public enum FitFieldBaseType {
    ENUM(0, 1, 0xFF),
    SINT8(1, 1, 0x7F),
    UINT8(2, 1, 0xFF),
    SINT16(3, 2, 0x7FFF),
    UINT16(4, 2, 0xFFFF),
    SINT32(5, 4, 0x7FFFFFFF),
    UINT32(6, 4, 0xFFFFFFFF),
    STRING(7, 1, ""),
    FLOAT32(8, 4, 0xFFFFFFFF),
    FLOAT64(9, 8, 0xFFFFFFFFFFFFFFFFL),
    UINT8Z(10, 1, 0),
    UINT16Z(11, 2, 0),
    UINT32Z(12, 4, 0),
    BYTE(13, 1, 0xFF),
    SINT64(14, 8, 0x7FFFFFFFFFFFFFFFL),
    UINT64(15, 8, 0xFFFFFFFFFFFFFFFFL),
    UINT64Z(16, 8, 0);

    public final int typeNumber;
    public final int size;
    public final int typeID;
    public final Object invalidValue;

    private static final SparseArray<FitFieldBaseType> typeForCode = new SparseArray<>(values().length);
    private static final SparseArray<FitFieldBaseType> typeForID = new SparseArray<>(values().length);

    static {
        for (FitFieldBaseType value : values()) {
            typeForCode.append(value.typeNumber, value);
            typeForID.append(value.typeID, value);
        }
    }

    FitFieldBaseType(int typeNumber, int size, Object invalidValue) {
        this.typeNumber = typeNumber;
        this.size = size;
        this.invalidValue = invalidValue;
        this.typeID = size > 1 ? (typeNumber | 0x80) : typeNumber;
    }

    public static FitFieldBaseType decodeTypeID(int typeNumber) {
        final FitFieldBaseType type = typeForID.get(typeNumber);
        if (type == null) {
            throw new IllegalArgumentException("Unknown type " + typeNumber);
        }
        return type;
    }
}
