package nodomain.freeyourgadget.gadgetbridge.pebble;

import android.net.Uri;

import nodomain.freeyourgadget.gadgetbridge.AbstractBTDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.protocol.GBDeviceProtocol;

public class PebbleSupport extends AbstractBTDeviceSupport {

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new PebbleProtocol();
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new PebbleIoThread(PebbleSupport.this, getDevice(), getDeviceProtocol(), getBluetoothAdapter(), getContext());
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onInstallApp(Uri uri) {
        getDeviceIOThread().installApp(uri);
    }

    @Override
    public synchronized PebbleIoThread getDeviceIOThread() {
        return (PebbleIoThread) super.getDeviceIOThread();
    }

    @Override
    public void onSetAlarms() {
        //nothing to do ATM
    }
}
