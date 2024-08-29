package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore.liberty4_nc;

import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class SoundcoreLiberty4NCDeviceSupport extends AbstractSerialDeviceSupport {

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
        return new SoundcoreLibertyIOThread(getDevice(), getContext(),
                (SoundcoreLibertyProtocol) getDeviceProtocol(),
                SoundcoreLiberty4NCDeviceSupport.this, getBluetoothAdapter());
    }

}
