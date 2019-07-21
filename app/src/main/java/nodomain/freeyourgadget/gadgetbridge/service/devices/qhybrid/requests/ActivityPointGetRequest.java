package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests;

import android.bluetooth.BluetoothGattCharacteristic;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ActivityPointGetRequest extends Request {
    public int activityPoint;

    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 6};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);

        byte[] value = characteristic.getValue();

        if (value.length != 6) return;

        ByteBuffer wrap = ByteBuffer.wrap(value);
        wrap.order(ByteOrder.LITTLE_ENDIAN);
        activityPoint = wrap.getInt(2) >> 8;
    }
}
