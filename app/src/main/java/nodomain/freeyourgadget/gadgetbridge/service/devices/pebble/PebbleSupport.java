package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.net.Uri;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
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
        return new PebbleProtocol(getDevice());
    }

    @Override
    protected GBDeviceIoThread createDeviceIOThread() {
        return new PebbleIoThread(PebbleSupport.this, getDevice(), getDeviceProtocol(), getBluetoothAdapter(), getContext());
    }

    @Override
    public boolean useAutoConnect() {
        return true;
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
    public void onSetConstantVibration(int intensity) {

    }

    @Override
    public synchronized PebbleIoThread getDeviceIOThread() {
        return (PebbleIoThread) super.getDeviceIOThread();
    }

    private boolean reconnect() {
        if (!isConnected() && useAutoConnect()) {
            if (getDevice().getState() == GBDevice.State.WAITING_FOR_RECONNECT) {
                gbDeviceIOThread.quit();
                gbDeviceIOThread.interrupt();
                gbDeviceIOThread = null;
                if (!connect()) {
                    return false;
                }
                try {
                    Thread.sleep(4000); // this is about the time the connect takes, so the notification can come though
                } catch (InterruptedException ignored) {
                }
            }
        }
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (reconnect()) {
            super.onNotification(notificationSpec);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (reconnect()) {
            super.onSetCallState(callSpec);
        }
    }

    @Override
    public void onSetMusicState(MusicStateSpec musicStateSpec) {
        if (reconnect()) {
            super.onSetMusicState(musicStateSpec);
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if (reconnect()) {
            super.onSetMusicInfo(musicSpec);
        }
    }


    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        //nothing to do ATM
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        if (reconnect()) {
            super.onAddCalendarEvent(calendarEventSpec);
        }
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        if (reconnect()) {
            super.onDeleteCalendarEvent(type, id);
        }
    }
}
