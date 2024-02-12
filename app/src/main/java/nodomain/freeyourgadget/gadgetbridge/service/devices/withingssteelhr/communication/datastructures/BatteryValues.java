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

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

public class BatteryValues extends WithingsStructure {

    private short percent;
    private short status;
    private int volt;

    public short getPercent() {
        return percent;
    }

    public void setPercent(short percent) {
        this.percent = percent;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public int getVolt() {
        return volt;
    }

    public void setVolt(int volt) {
        this.volt = volt;
    }

    @Override
    public short getLength() {
        return 14;
    }

    @Override
    public void fillFromRawDataAsBuffer(ByteBuffer rawDataBuffer) {
        percent = (short)(rawDataBuffer.get() & 255);
        status = (short)(rawDataBuffer.get() & 255);
        volt = rawDataBuffer.getInt();
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
    }

    @Override
    public short getType() {
        return WithingsStructureType.BATTERY_STATUS;
    }
}
