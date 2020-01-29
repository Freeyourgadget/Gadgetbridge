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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.ResultCode;

public class FileDeleteRequest extends FossilRequest {
    private boolean finished = false;
    private short handle;

    public FileDeleteRequest(short handle) {
        this.handle = handle;

        ByteBuffer buffer = createBuffer();

        buffer.putShort(handle);

        this.data = buffer.array();
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
        if(!characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d"))
            throw new RuntimeException("wrong response UUID");
        byte[] value = characteristic.getValue();

        if(value.length != 4) throw new RuntimeException("wrong response length");

        if(value[0] != (byte) 0x8B) throw new RuntimeException("wrong response start");

        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        if(buffer.getShort(1) != this.handle) throw new RuntimeException("wrong response handle");

        byte status = buffer.get(3);
        ResultCode code = ResultCode.fromCode(status);
        if(!code.inidicatesSuccess()) throw new RuntimeException("wrong response status: " + code + "(" + status + ")");

        this.finished = true;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{(byte) 0x0B};
    }

    @Override
    public int getPayloadLength() {
        return 3;
    }
}
