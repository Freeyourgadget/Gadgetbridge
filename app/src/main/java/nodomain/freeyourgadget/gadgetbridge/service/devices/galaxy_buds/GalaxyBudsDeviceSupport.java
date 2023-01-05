package nodomain.freeyourgadget.gadgetbridge.service.devices.galaxy_buds;

import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class GalaxyBudsDeviceSupport extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(GalaxyBudsDeviceSupport.class);


    @Override
    public void onSendConfiguration(String config) {
        super.onSendConfiguration(config);
    }

    @Override
    public void onTestNewFunction() {
        super.onTestNewFunction();
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        return true;
    }

    @Override
    public synchronized GalaxyBudsIOThread getDeviceIOThread() {
        return (GalaxyBudsIOThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    protected GBDeviceProtocol createDeviceProtocol() {
        return new GalaxyBudsProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new GalaxyBudsIOThread(getDevice(), getContext(), (GalaxyBudsProtocol) getDeviceProtocol(),
                GalaxyBudsDeviceSupport.this, getBluetoothAdapter());
    }
}
