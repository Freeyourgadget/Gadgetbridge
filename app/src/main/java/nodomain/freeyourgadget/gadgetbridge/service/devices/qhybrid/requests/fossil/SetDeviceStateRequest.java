package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SetDeviceStateRequest extends FossilRequest {
    private GBDevice.State deviceState;

    public SetDeviceStateRequest(GBDevice.State deviceState) {
        this.deviceState = deviceState;
    }

    public GBDevice.State getDeviceState() {
        return deviceState;
    }

    @Override
    public boolean isFinished() {
        return true;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[0];
    }
}
