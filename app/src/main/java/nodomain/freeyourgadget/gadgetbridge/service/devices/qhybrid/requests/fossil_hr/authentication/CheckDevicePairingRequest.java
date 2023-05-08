package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;

public class CheckDevicePairingRequest extends FossilRequest {
    protected boolean isFinished = false;

    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x01, 0x16};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        if(!characteristic.getUuid().equals(getRequestUUID())){
            throw new RuntimeException("wrong characteristic responded to pairing");
        }
        byte[] value = characteristic.getValue();
        if(value.length != 3){
            throw new RuntimeException("wrong pairing response length");
        }
        if(value[0] != 0x03 || value[1] != 0x16){
            throw new RuntimeException("wrong pairing response bytes");
        }
        this.onResult(value[2] == 0x01);
        this.isFinished = true;
    }

    public void onResult(boolean confirmationSuccess){};

    @Override
    public boolean isFinished() {
        return isFinished;
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0002-957f-7d4a-34a6-74696673696d");
    }
}
