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

public class WorkoutScreen extends WithingsStructure {

    public static final byte MODE_PACE = 2;
    public static final byte MODE_SPEED = 3;
    public static final byte MODE_ELSE = 1;

    public int id;

    public byte yetunknown1 = 0;

    public String name;

    public byte mode = MODE_PACE;

    public short yetunknown2 = 0;

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMode(byte mode) {
        this.mode = mode;
    }

    @Override
    public short getType() {
        return WithingsStructureType.WORKOUT_SCREEN_SETTINGS;
    }

    @Override
    public short getLength() {
        return (short) ((name != null ? name.getBytes().length : 0) + 9 + HEADER_SIZE);
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer rawDataBuffer) {
        rawDataBuffer.putInt(id);
        rawDataBuffer.put(yetunknown1);
        addStringAsBytesWithLengthByte(rawDataBuffer, name);
        rawDataBuffer.put(mode);
        rawDataBuffer.putShort(yetunknown2);
    }
}
