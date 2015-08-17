package nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;

public interface MiBandOperation {
    public void perform() throws IOException;
}
