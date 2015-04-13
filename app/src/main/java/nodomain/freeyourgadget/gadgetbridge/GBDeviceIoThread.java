package nodomain.freeyourgadget.gadgetbridge;

import android.content.Context;

public abstract class GBDeviceIoThread extends Thread {
    protected final GBDevice gbDevice;
    private final Context context;

    public GBDeviceIoThread(GBDevice gbDevice, Context context) {
        this.gbDevice = gbDevice;
        this.context = context;
    }

    public Context getContext() {
        return context;
    }

    public GBDevice getDevice() {
        return gbDevice;
    }

    protected boolean connect(String btDeviceAddress) {
        return false;
    }

    public void run() {
    }

    synchronized public void write(byte[] bytes) {
    }

    public void quit() {
    }
}