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

public class GetActivitySamples extends WithingsStructure {

    public long timestampFrom;

    public short maxSampleCount;

    public GetActivitySamples(long timestampFrom, short maxSampleCount) {
        this.timestampFrom = timestampFrom;
        this.maxSampleCount = maxSampleCount;
    }

    @Override
    public short getLength() {
        return 10;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        buffer.putInt((int)(timestampFrom & 4294967295L));
        buffer.putShort((short)maxSampleCount);
    }

    @Override
    public short getType() {
        return WithingsStructureType.GET_ACTIVITY_SAMPLES;
    }
}
