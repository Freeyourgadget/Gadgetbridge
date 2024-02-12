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
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

public class ActivitySampleTime extends WithingsStructure {

    private Date date;

    public Date getDate() {
        return date;
    }

    @Override
    public short getLength() {
        return 8;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {

    }

    @Override
    public void fillFromRawDataAsBuffer(ByteBuffer rawDataBuffer) {
        long timestampInSeconds = rawDataBuffer.getInt() & 4294967295L;
        date = new Date(timestampInSeconds * 1000);
    }

    @Override
    public short getType() {
        return WithingsStructureType.ACTIVITY_SAMPLE_TIME;
    }
}
