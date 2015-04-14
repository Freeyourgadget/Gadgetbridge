package nodomain.freeyourgadget.gadgetbridge.pebble;

import nodomain.freeyourgadget.gadgetbridge.AbstractBTDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;
import nodomain.freeyourgadget.gadgetbridge.protocol.PebbleProtocol;

public class PebbleSupport extends AbstractBTDeviceSupport {

    @Override
    public boolean connect() {
        // TODO: state and notification handling should move to IO thread
        getDevice().setState(GBDevice.State.CONNECTING);
        getDevice().sendDeviceUpdateIntent(getContext());
        getDeviceIOThread().start();
        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new PebbleProtocol();
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new PebbleIoThread(getDevice(), getDeviceProtocol(), getBluetoothAdapter(), getContext());
    }
}
