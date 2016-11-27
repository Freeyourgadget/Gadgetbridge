package nodomain.freeyourgadget.gadgetbridge.service.serial;

import android.content.Context;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

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

    protected boolean connect() {
        return false;
    }

    public void run() {
    }

    synchronized public void write(byte[] bytes) {
    }

    public void quit() {
    }
}