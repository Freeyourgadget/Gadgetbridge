package nodomain.freeyourgadget.gadgetbridge.service.devices.xm3;

import android.net.Uri;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class Xm3Support extends AbstractSerialDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(Xm3Support.class);

    @Override
    public boolean connect() {
        getDeviceIOThread().start();

        return true;
    }

    @Override
    protected GBDeviceProtocol createDeviceProtocol() {
        return new Xm3Protocol(getDevice());
    }

    @Override
    public void onSendConfiguration(final String config) {
        LOG.debug("onSendConfiguration " + config);

        Xm3IoThread xm3IoThread = getDeviceIOThread();
        Xm3Protocol xm3Protocol = (Xm3Protocol) getDeviceProtocol();

        switch (config) {
            case "test_xm3":
                break;
            default:
                LOG.error("Invalid Xm3 configuration " + config);
                break;
        }
    }

    @Override
    public void onTestNewFunction() {
        Xm3IoThread xm3IoThread = getDeviceIOThread();
        Xm3Protocol xm3Protocol = (Xm3Protocol) getDeviceProtocol();
        xm3IoThread.write(xm3Protocol.encodeTestNewFunction());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new Xm3IoThread(getDevice(), getContext(), (Xm3Protocol) getDeviceProtocol(), Xm3Support.this, getBluetoothAdapter());
    }

    @Override
    public synchronized Xm3IoThread getDeviceIOThread() {
        return (Xm3IoThread) super.getDeviceIOThread();
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onInstallApp(Uri uri) {
        // Nothing to do
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config, Integer id) {
        // Nothing to do
    }

    @Override
    public void onHeartRateTest() {
        // Nothing to do
    }

    @Override
    public void onSetConstantVibration(int intensity) {
        // Nothing to do
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        // Nothing to do
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        // Nothing to do
    }
}
