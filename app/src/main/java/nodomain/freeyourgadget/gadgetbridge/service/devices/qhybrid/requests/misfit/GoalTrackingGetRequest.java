package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GoalTrackingGetRequest extends Request {
    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if(value.length != 5) return;

        ByteBuffer buffer = ByteBuffer.wrap(value);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        short id = buffer.get(3);

        boolean state = buffer.get(4) == 1;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{01, 20, 01};
    }
}
