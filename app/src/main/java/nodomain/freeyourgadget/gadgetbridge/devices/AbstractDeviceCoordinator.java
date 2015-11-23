package nodomain.freeyourgadget.gadgetbridge.devices;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public abstract class AbstractDeviceCoordinator implements DeviceCoordinator {
    public boolean allowFetchActivityData(GBDevice device) {
        return device.isInitialized() && !device.isBusy() && supportsActivityDataFetching();
    }
}
