/*  Copyright (C) 2023-2024 Frank Ertl

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;

public class UserUnit extends WithingsStructure {

    private byte unknown1 = 0;
    private byte unknown2 = 0;
    private short type;
    private short unit;

    public UserUnit() {}

    public UserUnit(short type, short unit) {
        this.type = type;
        this.unit = unit;
    }

    @Override
    public short getLength() {
        return 10;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        buffer.put(unknown1);
        buffer.put(unknown2);
        buffer.putShort(type);
        buffer.putShort(unit);
    }

    @Override
    public short getType() {
        return WithingsStructureType.USER_UNIT;
    }
}
