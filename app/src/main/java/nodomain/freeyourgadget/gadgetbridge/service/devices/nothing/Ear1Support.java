package nodomain.freeyourgadget.gadgetbridge.service.devices.nothing;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class Ear1Support extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Ear1Support.class);


    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        //getDeviceIOThread().write(((NothingProtocol) getDeviceProtocol()).encodeBatteryStatusReq());
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public synchronized NothingIOThread getDeviceIOThread() {
        return (NothingIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected GBDeviceProtocol createDeviceProtocol() {
        return new NothingProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new NothingIOThread(getDevice(), getContext(), (NothingProtocol) getDeviceProtocol(),
                Ear1Support.this, getBluetoothAdapter());
    }
}
