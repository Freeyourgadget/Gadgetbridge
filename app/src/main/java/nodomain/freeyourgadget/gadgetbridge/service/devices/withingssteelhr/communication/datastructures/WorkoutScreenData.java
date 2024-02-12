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

public class WorkoutScreenData extends WithingsStructure {

    public long id;
    public short version;
    public String name;
    public short faceMode;
    public int flag;

    @Override
    public short getLength() {
        return (short) ((name != null ? name.getBytes().length : 0) + 13);
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {

    }

    @Override
    protected void fillFromRawDataAsBuffer(ByteBuffer rawDataBuffer) {
        this.id = rawDataBuffer.getInt() & 4294967295L;
        this.version = (short) (rawDataBuffer.get() & 255);
        this.name = getNextString(rawDataBuffer);
        this.faceMode = (short) (rawDataBuffer.get() & 255);
        this.flag = rawDataBuffer.getShort() & 65535;
    }

    @Override
    public short getType() {
        return WithingsStructureType.WORKOUT_SCREEN_DATA;
    }
}
