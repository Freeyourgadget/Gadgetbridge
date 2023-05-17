package nodomain.freeyourgadget.gadgetbridge.service.devices.bicycle_sensor.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.BinarySensorSupport;

public class CyclingSensorBaseSupport extends AbstractBTLEDeviceSupport {
    public CyclingSensorBaseSupport(Logger logger) {
        super(logger);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }
}
