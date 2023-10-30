package nodomain.freeyourgadget.gadgetbridge.service.devices.unknown;

import nodomain.freeyourgadget.gadgetbridge.service.AbstractDeviceSupport;

public class UnknownDeviceSupport extends AbstractDeviceSupport {
    @Override
    public boolean connect() {
        return false;
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
