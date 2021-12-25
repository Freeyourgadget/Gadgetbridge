package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.authentication;

import android.bluetooth.BluetoothGattCharacteristic;

public abstract class CheckDeviceNeedsConfirmationRequest extends AuthenticationRequest {
    private boolean isFinished = false;
    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x01, 0x07};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        if(!characteristic.getUuid().equals(getRequestUUID())){
            throw new RuntimeException("wrong characteristic responded to authentication");
        }
        byte[] value = characteristic.getValue();
        if(value.length != 3){
            throw new RuntimeException("wrong authentication response length");
        }
        if(value[0] != 0x03 || value[1] != 0x07){
            throw new RuntimeException("wrong authentication response bytes");
        }
        this.onResult(value[2] == 0x00);
        this.isFinished = true;
    }

    public abstract void onResult(boolean needsConfirmation);

    @Override
    public boolean isFinished() {
        return isFinished;
    }
}
