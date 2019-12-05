package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class PutSettingsFileRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[0];
    }
}
