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
import java.util.zip.CRC32;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.ResultCode;

public class DownloadFileRequest extends FileRequest {
    ByteBuffer buffer = null;
    public byte[] file = null;
    public int fileHandle;
    public int size;
    public long timeStamp;

    public DownloadFileRequest(short handle){
        init(handle, 0, 65535);
    }

    public DownloadFileRequest(short handle, int offset, int length) {
        init(handle, offset, length);
    }

    private void init(short handle, int offset, int length) {
        ByteBuffer buffer = createBuffer();
        buffer.putShort(handle);
        buffer.putInt(offset);
        buffer.putInt(length);
        this.data = buffer.array();
        this.fileHandle = handle;
        this.timeStamp = System.currentTimeMillis();
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{1};
    }

    @Override
    public int getPayloadLength() {
        return 11;
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
        byte[] data = characteristic.getValue();
        if(characteristic.getUuid().toString().equals("3dda0003-957f-7d4a-34a6-74696673696d")){
            if(buffer == null){
                buffer = ByteBuffer.allocate(4096);
                ByteBuffer buffer1 = ByteBuffer.wrap(data);
                buffer1.order(ByteOrder.LITTLE_ENDIAN);
                this.status = buffer1.get(3);
                short realHandle = buffer1.getShort(1);
                ResultCode code = ResultCode.fromCode(status);
                if(!code.inidicatesSuccess()){
                    log("wrong status: " + code + "   (" + status + ")");
                }else if(realHandle != fileHandle){
                    log("wrong handle: " + realHandle);
                    completed = true;
                }else{
                    log("handle: " + realHandle);
                }
            }else{
                completed = true;
            }
        }else if(characteristic.getUuid().toString().equals("3dda0004-957f-7d4a-34a6-74696673696d")){
            buffer.put(data, 1, data.length - 1);
            if((data[0] & -128) != 0){
                ByteBuffer buffer1 = ByteBuffer.allocate(buffer.position());
                buffer1.put(buffer.array(), 0, buffer.position());
                buffer1.order(ByteOrder.LITTLE_ENDIAN);
                file = buffer1.array();
                CRC32 crc = new CRC32();
                crc.update(file, 0, file.length - 4);
                this.size = file.length;
                log("file content: " + bytesToString(file));
                if(crc.getValue() != cutBits(buffer1.getInt(size - 4))){
                    log("checksum invalid    expected: " + buffer1.getInt(size - 4) + "   actual: " + crc.getValue());
                }
            }
        }
    }

    long cutBits(int value) {
        return value & 0b11111111111111111111111111111111L;

    }

    private String bytesToString(byte[] bytes){
        StringBuilder s = new StringBuilder();
        String chars = "0123456789ABCDEF";
        for (byte b : bytes) {
            s.append(chars.charAt((b >> 4) & 0xF));
            s.append(chars.charAt((b >> 0) & 0xF));
        }
        return s.toString();
    }
}
