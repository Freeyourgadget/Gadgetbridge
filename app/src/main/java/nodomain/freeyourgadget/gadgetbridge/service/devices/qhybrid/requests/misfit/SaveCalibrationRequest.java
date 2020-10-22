package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class SaveCalibrationRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x02, (byte) 0xF2, 0x0E};
    }
}
