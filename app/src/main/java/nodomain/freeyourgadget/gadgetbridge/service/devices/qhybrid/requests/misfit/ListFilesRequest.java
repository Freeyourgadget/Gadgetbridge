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

public class ListFilesRequest extends FileRequest{
    public int fileCount = -1;
    public int size = 0;
    private ByteBuffer buffer = null;
    private int length = 0;


    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        byte[] value = characteristic.getValue();

        if(uuid.equals("3dda0004-957f-7d4a-34a6-74696673696d")){
            buffer.put(value, 1, value.length - 1);
            length += value.length - 1;
            if((value[0] & -128) != 0){
                ByteBuffer buffer2 = ByteBuffer.wrap(buffer.array(), 0, length);
                buffer2.order(ByteOrder.LITTLE_ENDIAN);
                fileCount = buffer2.get(0);
                size = buffer2.getInt(1);
            }
        }else if(uuid.equals("3dda0003-957f-7d4a-34a6-74696673696d")){
            if(buffer == null){
                buffer = ByteBuffer.allocate(128);
            }else{
                completed = true;
            }
        }
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{(byte)5};
    }
}
