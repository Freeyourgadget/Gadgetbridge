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

public class FileVerifyRequest extends FossilRequest {
    private boolean isFinished = false;
    private short handle;

    public FileVerifyRequest(short fileHandle) {
        this.handle = fileHandle;
        ByteBuffer buffer = this.createBuffer();
        buffer.putShort(fileHandle);

        this.data = buffer.array();
    }

    public short getHandle() {
        return handle;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);

        if(!characteristic.getUuid().toString().equals(this.getRequestUUID().toString())){
            throw new RuntimeException("wrong response UUID");
        }

        byte[] value = characteristic.getValue();

        byte type = (byte)(value[0] & 0x0F);

        if(type == 0x0A) return;

        if(type != 4) throw new RuntimeException("wrong response type");

        if(value.length != 4) throw new RuntimeException("wrong response length");

        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        if(this.handle != buffer.getShort(1)) throw new RuntimeException("wrong response handle");

        byte status = buffer.get(3);

        ResultCode code = ResultCode.fromCode(status);
        if(!code.inidicatesSuccess()) throw new RuntimeException("wrong response status: " + code + "   (" + status + ")");

        this.isFinished = true;

        this.onPrepare();
    }

    public void onPrepare(){}

    @Override
    public byte[] getStartSequence() {
        return new byte[]{4};
    }

    @Override
    public int getPayloadLength() {
        return 3;
    }

    @Override
    public boolean isFinished(){
        return this.isFinished;
    }

}
