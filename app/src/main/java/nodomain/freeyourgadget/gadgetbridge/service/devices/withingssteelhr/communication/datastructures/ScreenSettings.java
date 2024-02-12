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

public class ScreenSettings extends WithingsStructure {

    private int id;

    // TODO change to an actual unique ID. Must then be changed in User too.
    private int userId = 123456;
    private int yetUnknown1 = 0;
    private int yetUnknown2 = 0;
    private byte idOnDevice;
    private byte yetUnkown3 = 0x01;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte getIdOnDevice() {
        return idOnDevice;
    }

    public void setIdOnDevice(byte idOnDevice) {
        this.idOnDevice = idOnDevice;
    }

    @Override
    public short getLength() {
        return 22;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        buffer.putInt(this.id);
        buffer.putInt(this.userId);
        buffer.putInt(this.yetUnknown1);
        buffer.putInt(this.yetUnknown2);
        buffer.put(this.idOnDevice);
        buffer.put(this.yetUnkown3);
    }

    @Override
    public short getType() {
        return WithingsStructureType.SCREEN_SETTINGS;
    }
}
