package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.net.Uri;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.serial.AbstractSerialDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceIoThread;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

public class PebbleSupport extends AbstractSerialDeviceSupport {

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
        getDeviceIOThread().installApp(uri, 0);
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config) {
        try {
            ArrayList<Pair<Integer, Object>> pairs = new ArrayList<>();

            JSONObject json = new JSONObject(config);
            Iterator<String> keysIterator = json.keys();
            while (keysIterator.hasNext()) {
                String keyStr = keysIterator.next();
                Object object = json.get(keyStr);
                pairs.add(new Pair<>(Integer.parseInt(keyStr), object));
            }
            getDeviceIOThread().write(((PebbleProtocol) getDeviceProtocol()).encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, uuid, pairs));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartrate(boolean enable) {

    }

    @Override
    public synchronized PebbleIoThread getDeviceIOThread() {
        return (PebbleIoThread) super.getDeviceIOThread();
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        //nothing to do ATM
    }
}
