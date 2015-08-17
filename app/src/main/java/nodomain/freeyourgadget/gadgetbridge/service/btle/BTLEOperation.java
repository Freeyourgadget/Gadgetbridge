package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.MiBandSupport;

public interface BTLEOperation {
    public void perform() throws IOException;
}
