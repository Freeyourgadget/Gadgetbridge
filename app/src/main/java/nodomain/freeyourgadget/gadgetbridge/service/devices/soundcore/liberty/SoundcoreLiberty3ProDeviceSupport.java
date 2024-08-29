package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SoundcoreLiberty3ProDeviceSupport extends AbstractSerialDeviceSupport {
    public static final UUID UUID_DEVICE_CTRL = UUID.fromString("0cf12d31-fac3-4553-bd80-d6832e7b3952");

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new SoundcoreLibertyProtocol(getDevice());
    }

    @Override
    protected synchronized GBDeviceIoThread createDeviceIOThread() {
        return new SoundcoreLibertyIOThread(
                getDevice(),
                getContext(),
                (SoundcoreLibertyProtocol) getDeviceProtocol(),
                UUID_DEVICE_CTRL,
                this,
                getBluetoothAdapter()
        );
    }
}
