package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class SetTripleTapEnabledRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{2, 7, 3, 1};
    }
}
