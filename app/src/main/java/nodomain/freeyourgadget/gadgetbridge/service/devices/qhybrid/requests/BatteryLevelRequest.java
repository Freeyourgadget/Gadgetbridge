package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BatteryLevelRequest extends Request {
    public short level = -1;
    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 8};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);

        byte[] value = characteristic.getValue();

        if (value.length >= 3) {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            level = buffer.get(2);
        }
    }
}
