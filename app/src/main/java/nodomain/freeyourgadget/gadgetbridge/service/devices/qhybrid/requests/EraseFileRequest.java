package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

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
            Log.d(getName(), "wrong descriptor");
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
