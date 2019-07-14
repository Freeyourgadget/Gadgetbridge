package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetStepGoalRequest extends Request {
    public int stepGoal = -1;
    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
        byte[] value = characteristic.getValue();
        if (value.length < 6) {
            return;
        } else {
            ByteBuffer buffer = ByteBuffer.wrap(value);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            stepGoal = buffer.getInt(2);
        }
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 16};
    }
}
