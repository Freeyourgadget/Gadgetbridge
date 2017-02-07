package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.net.Uri;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
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
                if (object instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) object;
                    byte[] byteArray = new byte[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++) {
                        byteArray[i] = ((Integer) jsonArray.get(i)).byteValue();
                    }
                    object = byteArray;
                }
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
        String currentPrivacyMode = GBApplication.getPrefs().getString("pebble_pref_privacy_mode", getContext().getString(R.string.p_pebble_privacy_mode_off));
        if (getContext().getString(R.string.p_pebble_privacy_mode_complete).equals(currentPrivacyMode)) {
            notificationSpec.body = null;
            notificationSpec.sender = null;
            notificationSpec.subject = null;
            notificationSpec.title = null;
            notificationSpec.phoneNumber = null;
        } else if (getContext().getString(R.string.p_pebble_privacy_mode_content).equals(currentPrivacyMode)) {
            notificationSpec.sender = "\n\n\n\n\n" + notificationSpec.sender;
        }
        if (reconnect()) {
            super.onNotification(notificationSpec);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (reconnect()) {
            if ((callSpec.command != CallSpec.CALL_OUTGOING) || GBApplication.getPrefs().getBoolean("pebble_enable_outgoing_call", true)) {
                super.onSetCallState(callSpec);
            }
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

    @Override
    public void onSendConfiguration(String config) {
        if (reconnect()) {
            super.onSendConfiguration(config);
        }
    }

    @Override
    public void onTestNewFunction() {
        if (reconnect()) {
            super.onTestNewFunction();
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        if (reconnect()) {
            super.onSendWeather(weatherSpec);
        }
    }
}
