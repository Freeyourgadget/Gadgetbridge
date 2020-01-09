/*  Copyright (C) 2019-2020 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class OTAEraseRequest extends Request {

    public OTAEraseRequest(int pageOffset) {
        ByteBuffer buffer = createBuffer();
        buffer.putShort((short) 23131);
        buffer.putInt(pageOffset);

        this.data = buffer.array();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{18};
    }

    @Override
    public int getPayloadLength() {
        return 7;
    }


    public UUID getRequestUUID(){
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] bytes = characteristic.getValue();
        final ByteBuffer wrap = ByteBuffer.wrap(bytes);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        short fileHandle = wrap.getShort(1);
        byte status = wrap.get(3);
        int sizeWritten = wrap.getInt(4);
    }
}
