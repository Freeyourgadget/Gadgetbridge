package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class PrepareFilesRequestOrWhatever extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{(byte) 0x09, (byte) 0xFF, (byte) 0xFF};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0003-957f-7d4a-34a6-74696673696d");
    }
}
