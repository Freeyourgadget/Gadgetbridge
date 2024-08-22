/*  Copyright (C) 2015-2024 Andreas Shimokawa, Arjan Schrijver, Carsten
    Pfeiffer, Daniele Gobbetti, Kasha, Sebastian Kranz, Steffen Liebergeld,
    Taavi Eomäe

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.content.IntentFilter;
import android.net.Uri;
import android.util.Pair;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.externalevents.AlarmReceiver;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
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
    private static final Logger LOG = LoggerFactory.getLogger(PebbleSupport.class);
    private AlarmReceiver mAlarmReceiver = null;

    @Override
    public void dispose() {
        super.dispose();
        unregisterSunriseSunsetAlarmReceiver();
    }

    private void registerSunriseSunsetAlarmReceiver() {
        if (!getDevicePrefs().getBoolean("send_sunrise_sunset", false)) {
            LOG.info("won't register sunrise and sunset receiver (disabled in preferences)");
            return;
        }
        unregisterSunriseSunsetAlarmReceiver();
        LOG.info("registering sunrise and sunset receiver");
        this.mAlarmReceiver = new AlarmReceiver();
        ContextCompat.registerReceiver(GBApplication.getContext(), mAlarmReceiver, new IntentFilter("DAILY_ALARM"), ContextCompat.RECEIVER_EXPORTED);
    }

    private void unregisterSunriseSunsetAlarmReceiver() {
        if (mAlarmReceiver != null) {
            GBApplication.getContext().unregisterReceiver(mAlarmReceiver);
            mAlarmReceiver = null;
        }
    }

    @Override
    public boolean connect() {
        getDeviceIOThread().start();
        registerSunriseSunsetAlarmReceiver();
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
        PebbleProtocol pebbleProtocol = (PebbleProtocol) getDeviceProtocol();
        PebbleIoThread pebbleIoThread = getDeviceIOThread();
        // Catch fake URLs first
        if (uri.equals(Uri.parse("fake://health"))) {
            getDeviceIOThread().write(pebbleProtocol.encodeActivateHealth(true));
            String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, getContext().getString(R.string.p_unit_metric));
            if (units.equals(getContext().getString(R.string.p_unit_metric))) {
                pebbleIoThread.write(pebbleProtocol.encodeSetSaneDistanceUnit(true));
            } else {
                pebbleIoThread.write(pebbleProtocol.encodeSetSaneDistanceUnit(false));
            }
            return;
        }
        if (uri.equals(Uri.parse("fake://hrm"))) {
            getDeviceIOThread().write(pebbleProtocol.encodeActivateHRM(true));
            return;
        }
        if (uri.equals(Uri.parse("fake://weather"))) {
            getDeviceIOThread().write(pebbleProtocol.encodeActivateWeather(true));
            return;
        }

        // it is a real app
        pebbleIoThread.installApp(uri, 0);
    }

    @Override
    public void onAppConfiguration(UUID uuid, String config, Integer id) {
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
                } else if (object instanceof Boolean) {
                    object = (short) (((Boolean) object) ? 1 : 0);
                } else if (object instanceof Double) {
                    object = ((Double) object).intValue();
                }
                pairs.add(new Pair<>(Integer.parseInt(keyStr), object));
            }
            getDeviceIOThread().write(((PebbleProtocol) getDeviceProtocol()).encodeApplicationMessagePush(PebbleProtocol.ENDPOINT_APPLICATIONMESSAGE, uuid, pairs, id));
        } catch (JSONException e) {
            LOG.error("Error while parsing JSON", e);
        }
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
        String currentPrivacyMode = GBApplication.getDevicePrefs(gbDevice.getAddress()).getString("pebble_pref_privacy_mode", getContext().getString(R.string.p_pebble_privacy_mode_off));
        if (getContext().getString(R.string.p_pebble_privacy_mode_complete).equals(currentPrivacyMode)) {
            notificationSpec.body = null;
            notificationSpec.sender = null;
            notificationSpec.subject = null;
            notificationSpec.title = null;
            notificationSpec.phoneNumber = null;
        } else if (getContext().getString(R.string.p_pebble_privacy_mode_content).equals(currentPrivacyMode)) {
            if (notificationSpec.sender != null) {
                notificationSpec.sender = "\n\n\n\n\n" + notificationSpec.sender;
            } else if (notificationSpec.title != null) {
                notificationSpec.title = "\n\n\n\n\n" + notificationSpec.title;
            } else if (notificationSpec.subject != null) {
                notificationSpec.subject = "\n\n\n\n\n" + notificationSpec.subject;
            }
        }
        if (reconnect()) {
            super.onNotification(notificationSpec);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        if (reconnect()) {
            if ((callSpec.command != CallSpec.CALL_OUTGOING) || GBApplication.getDevicePrefs(gbDevice.getAddress()).getBoolean("pebble_enable_outgoing_call", true)) {
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
    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        if (reconnect()) {
            super.onSendWeather(weatherSpecs);
        }
    }
}
