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
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EraseFileRequest extends FileRequest{
    public short fileHandle, deletedHandle;

    public EraseFileRequest(short handle) {
        fileHandle = handle;
        ByteBuffer buffer = createBuffer();
        buffer.putShort(handle);
        this.data = buffer.array();
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
        if(!characteristic.getUuid().toString().equals(getRequestUUID().toString())){
            log("wrong descriptor");
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(characteristic.getValue());
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        deletedHandle = buffer.getShort(1);
        status = buffer.get(3);

        log("file " + deletedHandle + " erased: " + status);
    }

    @Override
    public int getPayloadLength() {
        return 3;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{3};
    }
}
