package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class GetCountdownSettingsRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 19, 1};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length != 14) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        long startTime = j(buffer.getInt(3));
        long endTime = j(buffer.getInt(7));
        byte progress = buffer.get(13);
        short offset = buffer.getShort(11);

        log("progress: " + progress);

    }


    public static long j(final int n) {
        if (n < 0) {
            return 4294967296L + n;
        }
        return n;
    }
}
