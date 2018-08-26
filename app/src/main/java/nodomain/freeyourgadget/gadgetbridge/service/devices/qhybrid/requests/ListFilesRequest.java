package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.util.SparseArray;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

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
