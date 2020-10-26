package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class FactoryResetRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{(byte) 0x02, (byte) 0xF1, (byte) 0x23, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
    }
}
