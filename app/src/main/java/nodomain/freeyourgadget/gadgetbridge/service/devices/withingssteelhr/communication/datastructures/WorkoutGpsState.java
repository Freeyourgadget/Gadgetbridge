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

public class WorkoutGpsState extends WithingsStructure {

    private final boolean gpsEnabled;

    public WorkoutGpsState(boolean gpsEnabled) {
        this.gpsEnabled = gpsEnabled;
    }

    @Override
    public short getLength() {
        return 6;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        buffer.putShort(gpsEnabled? (short)1 : 0);
    }

    @Override
    public short getType() {
        return WithingsStructureType.WORKOUT_GPS_STATE;
    }
}
