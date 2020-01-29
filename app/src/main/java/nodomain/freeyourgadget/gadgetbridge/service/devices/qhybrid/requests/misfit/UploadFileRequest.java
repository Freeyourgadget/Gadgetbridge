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
import java.util.ArrayList;
import java.util.UUID;
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class UploadFileRequest extends Request {
    public enum UploadState{INITIALIZED, UPLOAD, UPLOADED, ERROR}

    public UploadState state;

    public ArrayList<byte[]> packets = new ArrayList<>();

    public UploadFileRequest(short handle, byte[] file) {
        int fileLength = file.length + 4;
        ByteBuffer buffer = this.createBuffer();
        buffer.putShort(1, handle);
        buffer.putInt(3, 0);
        buffer.putInt(7, fileLength);
        buffer.putInt(11, fileLength);

        this.data = buffer.array();

        prepareFilePackets(file);

        state = UploadState.INITIALIZED;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length == 4) {
            if (value[1] != 0) {
                state = UploadState.ERROR;
                return;
            }
            state = UploadState.UPLOAD;
        }else if(value.length == 9){
            if(value[1] != 0){
                state = UploadState.ERROR;
                return;
            }
            state = UploadState.UPLOADED;
        }
    }

    private void prepareFilePackets(byte[] file) {
        ByteBuffer buffer = ByteBuffer.allocate(file.length + 4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.put(file);

        CRC32 crc = new CRC32();
        crc.update(file);
        buffer.putInt((int) crc.getValue());

        byte[] fileFull = buffer.array();
        for (int i = 0, sequence = 0; i < fileFull.length + 4; i += 18, sequence++) {
            byte[] packet;
            if (i + 18 >= fileFull.length) {
                packet = new byte[fileFull.length - i + 2];
                System.arraycopy(fileFull, i, packet, 2, fileFull.length - i);
            } else {
                packet = new byte[20];
                System.arraycopy(fileFull, i, packet, 2, 18);
            }
            packet[0] = 0x12;
            packet[1] = (byte) sequence;
            packets.add(packet);
        }
        packets.get(0)[1] |= 0x40;
        if (packets.size() > 1) {
            packets.get(packets.size() - 1)[1] |= 0x80;
        }
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{17};
    }

    @Override
    public int getPayloadLength() {
        return 15;
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0007-957f-7d4a-34a6-74696673696d");
    }
}
