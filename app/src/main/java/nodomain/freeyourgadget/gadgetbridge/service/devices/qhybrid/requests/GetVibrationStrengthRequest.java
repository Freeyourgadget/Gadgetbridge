package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetVibrationStrengthRequest extends Request {
    public int strength = -1;

    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length < 4) {
            return;
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            strength = (int) buffer.get(3);
        }
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 15, 8};
    }
}
