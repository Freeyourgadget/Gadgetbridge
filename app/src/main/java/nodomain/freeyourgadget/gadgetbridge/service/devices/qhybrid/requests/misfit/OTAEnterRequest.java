package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class OTAEnterRequest extends Request {
    public boolean success = false;

    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, -15, 8};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] result = characteristic.getValue();
        success = result[2] == 9;
    }
}
