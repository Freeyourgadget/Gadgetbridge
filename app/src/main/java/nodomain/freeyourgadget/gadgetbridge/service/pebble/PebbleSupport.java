package nodomain.freeyourgadget.gadgetbridge.service.pebble;

import android.net.Uri;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.AbstractBTDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.GBDeviceProtocol;

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
    public void onSetAlarms(ArrayList<Alarm> alarms) {
        //nothing to do ATM
    }
}
