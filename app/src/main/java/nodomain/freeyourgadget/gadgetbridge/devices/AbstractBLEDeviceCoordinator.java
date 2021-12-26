package nodomain.freeyourgadget.gadgetbridge.devices;

public abstract class AbstractBLEDeviceCoordinator extends AbstractDeviceCoordinator{
    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.BLE;
    }
}
