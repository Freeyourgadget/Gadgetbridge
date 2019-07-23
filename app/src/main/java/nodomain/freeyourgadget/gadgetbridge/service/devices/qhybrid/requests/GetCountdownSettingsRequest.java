package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;

public class GetCountdownSettingsRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 19, 1};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if(value.length != 14){
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        int startTime = buffer.getInt(3);
        int endTime = buffer.getInt(7);
        short offset = buffer.getShort(11);
        short progress = buffer.getShort(13);
    }
}
