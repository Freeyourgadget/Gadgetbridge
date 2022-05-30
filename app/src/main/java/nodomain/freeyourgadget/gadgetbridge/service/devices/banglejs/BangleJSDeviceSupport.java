/*  Copyright (C) 2019-2021 Andreas Shimokawa, Gordon Williams

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Base64;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.lang.reflect.Field;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTERNET_ACCESS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTENTS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANGLEJS_TEXT_BITMAP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBHelper.*;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

public class BangleJSDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(BangleJSDeviceSupport.class);

    private BluetoothGattCharacteristic rxCharacteristic = null;
    private BluetoothGattCharacteristic txCharacteristic = null;
    private boolean allowHighMTU = false;
    private int mtuSize = 20;

    private String receivedLine = "";
    private boolean realtimeHRM = false;
    private boolean realtimeStep = false;
    private int realtimeHRMInterval = 30*60;

    // Local Intents - for app manager communication
    public static final String BANGLEJS_COMMAND_TX = "banglejs_command_tx";
    public static final String BANGLEJS_COMMAND_RX = "banglejs_command_rx";
    // Global Intents
    private static final String BANGLE_ACTION_UART_TX = "com.banglejs.uart.tx";

    public BangleJSDeviceSupport() {
        super(LOG);
        addSupportedService(BangleJSConstants.UUID_SERVICE_NORDIC_UART);

        registerLocalIntents();
        registerGlobalIntents();
    }

    private void registerLocalIntents() {
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(BANGLEJS_COMMAND_TX);
        BroadcastReceiver commandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BANGLEJS_COMMAND_TX: {
                        String data = String.valueOf(intent.getExtras().get("DATA"));
                        try {
                            TransactionBuilder builder = performInitialized("TX");
                            uartTx(builder, data);
                            builder.queue(getQueue());
                        } catch (IOException e) {
                            GB.toast(getContext(), "Error in TX: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                        }
                        break;
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(GBApplication.getContext()).registerReceiver(commandReceiver, commandFilter);
    }

    private void registerGlobalIntents() {
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(BANGLE_ACTION_UART_TX);
        BroadcastReceiver commandReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction()) {
                    case BANGLE_ACTION_UART_TX: {
                        /* In Tasker:
                          Action: com.banglejs.uart.tx
                          Cat: None
                          Extra: line:Terminal.println(%avariable)
                          Target: Broadcast Receiver

                          Variable: Number, Configure on Import, NOT structured, Value set, Nothing Exported, NOT Same as value
                         */
                        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
                        if (!devicePrefs.getBoolean(PREF_DEVICE_INTENTS, false)) return;
                        String data = intent.getStringExtra("line");
                        if (data==null) {
                            GB.toast(getContext(), "UART TX Intent, but no 'line' supplied", Toast.LENGTH_LONG, GB.ERROR);
                            return;
                        }
                        if (!data.endsWith("\n")) data += "\n";
                        try {
                            TransactionBuilder builder = performInitialized("TX");
                            uartTx(builder, data);
                            builder.queue(getQueue());
                        } catch (IOException e) {
                            GB.toast(getContext(), "Error in TX: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                        }
                        break;
                    }
                }
            }
        };
        GBApplication.getContext().registerReceiver(commandReceiver, commandFilter);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());
        gbDevice.setBatteryThresholdPercent((short) 30);

        rxCharacteristic = getCharacteristic(BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX);
        txCharacteristic = getCharacteristic(BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_TX);
        builder.setGattCallback(this);
        builder.notify(rxCharacteristic, true);

        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        allowHighMTU = devicePrefs.getBoolean(PREF_ALLOW_HIGH_MTU, false);

        uartTx(builder, " \u0003"); // clear active line

        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean("datetime_synconconnect", true))
          transmitTime(builder);
        //sendSettings(builder);

        // get version

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        LOG.info("Initialization Done");

        return builder;
    }

    /// Write a string of data, and chunk it up
    private void uartTx(TransactionBuilder builder, String str) {
        LOG.info("UART TX: " + str);
        byte[] bytes;
        bytes = str.getBytes(StandardCharsets.ISO_8859_1);
        // FIXME: somehow this is still giving us UTF8 data when we put images in strings. Maybe JSON.stringify is converting to UTF-8?
        for (int i=0;i<bytes.length;i+=mtuSize) {
            int l = bytes.length-i;
            if (l>mtuSize) l=mtuSize;
            byte[] packet = new byte[l];
            System.arraycopy(bytes, i, packet, 0, l);
            builder.write(txCharacteristic, packet);
        }
    }


    /// Write a string of data, and chunk it up
    public String jsonToString(JSONObject jsonObj) {
        String json = jsonObj.toString();
        // toString creates '\u0000' instead of '\0'
        // FIXME: there have got to be nicer ways of handling this - maybe we just make our own JSON.toString (see below)
        json = json.replaceAll("\\\\u000([01234567])", "\\\\$1");
        json = json.replaceAll("\\\\u00([0123456789abcdef][0123456789abcdef])", "\\\\x$1");
        return json;
        /*String json = "{";
        Iterator<String> iter = jsonObj.keys();
        while (iter.hasNext()) {
            String key = iter.next();
            Object v = jsonObj.get(key);
            if (v instanceof Integer) {
                // ...
            } else // ..
            if (iter.hasNext()) json+=",";
        }
        return json+"}";*/
    }

    /// Write a JSON object of data
    private void uartTxJSON(String taskName, JSONObject json) {
        try {
            TransactionBuilder builder = performInitialized(taskName);
            uartTx(builder, "\u0010GB("+jsonToString(json)+")\n");
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error in "+taskName+": " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    /// Write JSON object of the form {t:taskName, err:message}

    private void uartTxJSONError(String taskName, String message) {
        uartTxJSONError(taskName,message,null);
    }

    private void uartTxJSONError(String taskName, String message,String id) {
        JSONObject o = new JSONObject();
        try {
            o.put("t", taskName);
            if( id!=null)
                o.put("id", id);
            o.put("err", message);
        } catch (JSONException e) {
            GB.toast(getContext(), "uartTxJSONError: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
        uartTxJSON(taskName, o);
    }



    private void handleUartRxLine(String line) {
        LOG.info("UART RX LINE: " + line);

        if (">Uncaught ReferenceError: \"GB\" is not defined".equals(line))
          GB.toast(getContext(), "Gadgetbridge plugin not installed on Bangle.js", Toast.LENGTH_LONG, GB.ERROR);
        else if (line.length() > 0 && line.charAt(0)=='{') {
            // JSON - we hope!
            try {
                JSONObject json = new JSONObject(line);
                LOG.info("UART RX JSON parsed successfully");
                handleUartRxJSON(json);
            } catch (JSONException e) {
                LOG.info("UART RX JSON parse failure: "+ e.getLocalizedMessage());
                GB.toast(getContext(), "Malformed JSON from Bangle.js: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            }
        } else {
            LOG.info("UART RX line started with "+(int)line.charAt(0)+" - ignoring");
        }
    }

    private void handleUartRxJSON(JSONObject json) throws JSONException {
        String packetType = json.getString("t");
        switch (packetType) {
            case "info":
                GB.toast(getContext(), "Bangle.js: " + json.getString("msg"), Toast.LENGTH_LONG, GB.INFO);
                break;
            case "warn":
                GB.toast(getContext(), "Bangle.js: " + json.getString("msg"), Toast.LENGTH_LONG, GB.WARN);
                break;
            case "error":
                GB.toast(getContext(), "Bangle.js: " + json.getString("msg"), Toast.LENGTH_LONG, GB.ERROR);
                break;
            case "ver": {
                if (json.has("fw1"))
                    getDevice().setFirmwareVersion(json.getString("fw1"));
                if (json.has("fw2"))
                    getDevice().setFirmwareVersion2(json.getString("fw2"));
            } break;
            case "status": {
                GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
                if (json.has("bat")) {
                    int b = json.getInt("bat");
                    if (b < 0) b = 0;
                    if (b > 100) b = 100;
                    batteryInfo.level = b;
                    batteryInfo.state = BatteryState.BATTERY_NORMAL;
                }
                if (json.has("chg") && json.getInt("chg") == 1) {
                    batteryInfo.state = BatteryState.BATTERY_CHARGING;
                }
                if (json.has("volt"))
                    batteryInfo.voltage = (float) json.getDouble("volt");
                handleGBDeviceEvent(batteryInfo);
            } break;
            case "findPhone": {
                boolean start = json.has("n") && json.getBoolean("n");
                GBDeviceEventFindPhone deviceEventFindPhone = new GBDeviceEventFindPhone();
                deviceEventFindPhone.event = start ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.STOP;
                evaluateGBDeviceEvent(deviceEventFindPhone);
            } break;
            case "music": {
                GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.valueOf(json.getString("n").toUpperCase());
                evaluateGBDeviceEvent(deviceEventMusicControl);
            } break;
            case "call": {
                GBDeviceEventCallControl deviceEventCallControl = new GBDeviceEventCallControl();
                deviceEventCallControl.event = GBDeviceEventCallControl.Event.valueOf(json.getString("n").toUpperCase());
                evaluateGBDeviceEvent(deviceEventCallControl);
            } break;
            case "notify" : {
                GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();
                // .title appears unused
                deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.valueOf(json.getString("n").toUpperCase());
                if (json.has("id"))
                    deviceEvtNotificationControl.handle = json.getInt("id");
                if (json.has("tel"))
                    deviceEvtNotificationControl.phoneNumber = json.getString("tel");
                if (json.has("msg"))
                    deviceEvtNotificationControl.reply = json.getString("msg");
                evaluateGBDeviceEvent(deviceEvtNotificationControl);
            } break;
            case "act": {
                BangleJSActivitySample sample = new BangleJSActivitySample();
                sample.setTimestamp((int) (GregorianCalendar.getInstance().getTimeInMillis() / 1000L));
                int hrm = 0;
                int steps = 0;
                if (json.has("hrm")) hrm = json.getInt("hrm");
                if (json.has("stp")) steps = json.getInt("stp");
                int activity = BangleJSSampleProvider.TYPE_ACTIVITY;
                /*if (json.has("act")) {
                    String actName = "TYPE_" + json.getString("act").toUpperCase();
                    try {
                        Field f = ActivityKind.class.getField(actName);
                        try {
                            activity = f.getInt(null);
                        } catch (IllegalAccessException e) {
                            LOG.info("JSON activity '"+actName+"' not readable");
                        }
                    } catch (NoSuchFieldException e) {
                        LOG.info("JSON activity '"+actName+"' not found");
                    }
                }*/
                sample.setRawKind(activity);
                sample.setHeartRate(hrm);
                sample.setSteps(steps);
                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                    Long userId = getUser(dbHandler.getDaoSession()).getId();
                    Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
                    BangleJSSampleProvider provider = new BangleJSSampleProvider(getDevice(), dbHandler.getDaoSession());
                    sample.setDeviceId(deviceId);
                    sample.setUserId(userId);
                    provider.addGBActivitySample(sample);
                } catch (Exception ex) {
                    LOG.warn("Error saving activity: " + ex.getLocalizedMessage());
                }
                // push realtime data
                if (realtimeHRM || realtimeStep) {
                    Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                            .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
                    LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                }
            } break;
            case "http": {
                Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
                String _id=null;
                try {
                    _id = json.getString("id");
                } catch (JSONException e) {
                }
                final String id = _id;


                if (BuildConfig.INTERNET_ACCESS && devicePrefs.getBoolean(PREF_DEVICE_INTERNET_ACCESS, false)) {
                    RequestQueue queue = Volley.newRequestQueue(getContext());
                    String url = json.getString("url");

                    String _xmlPath = "";
                    try {
                        _xmlPath = json.getString("xpath");
                    } catch (JSONException e) {
                    }
                    final String xmlPath = _xmlPath;
                    // Request a string response from the provided URL.
                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                            new Response.Listener<String>() {
                                @Override
                                public void onResponse(String response) {
                                    JSONObject o = new JSONObject();
                                    if (xmlPath.length() != 0) {
                                        try {
                                            InputSource inputXML = new InputSource(new StringReader(response));
                                            XPath xPath = XPathFactory.newInstance().newXPath();
                                            response = xPath.evaluate(xmlPath, inputXML);
                                        } catch (Exception error) {
                                            uartTxJSONError("http", error.toString(),id);
                                            return;
                                        }
                                    }
                                    try {
                                        o.put("t", "http");
                                        if( id!=null)
                                            o.put("id", id);
                                        o.put("resp", response);
                                    } catch (JSONException e) {
                                        GB.toast(getContext(), "HTTP: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                                    }
                                    uartTxJSON("http", o);
                                }
                            }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            JSONObject o = new JSONObject();
                            uartTxJSONError("http", error.toString(),id);
                        }
                    });
                    queue.add(stringRequest);
                } else {
                    if (BuildConfig.INTERNET_ACCESS)
                        uartTxJSONError("http", "Internet access not enabled, check Gadgetbridge Device Settings",id);
                    else
                        uartTxJSONError("http", "Internet access not enabled in this Gadgetbridge build",id);
                }
            } break;
            case "intent": {
                Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
                if (devicePrefs.getBoolean(PREF_DEVICE_INTENTS, false)) {
                    String action = json.getString("action");
                    JSONObject extra = json.getJSONObject("extra");
                    Intent in = new Intent();
                    in.setAction(action);
                    if (extra != null) {
                        Iterator<String> iter = extra.keys();
                        while (iter.hasNext()) {
                            String key = iter.next();
                            in.putExtra(key, extra.getString(key));
                        }
                    }
                    LOG.info("Sending intent " + action);
                    this.getContext().getApplicationContext().sendBroadcast(in);
                } else {
                    uartTxJSONError("intent", "Android Intents not enabled, check Gadgetbridge Device Settings");
                }
            }
            default : {
                LOG.info("UART RX JSON packet type '"+packetType+"' not understood.");
            }
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }
        if (BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX.equals(characteristic.getUuid())) {
            byte[] chars = characteristic.getValue();
            // check to see if we get more data - if so, increase out MTU for sending
            if (allowHighMTU && chars.length > mtuSize)
                mtuSize = chars.length;
            String packetStr = new String(chars);
            LOG.info("RX: " + packetStr);
            receivedLine += packetStr;
            while (receivedLine.contains("\n")) {
                int p = receivedLine.indexOf("\n");
                String line =  receivedLine.substring(0,p-1);
                receivedLine = receivedLine.substring(p+1);
                handleUartRxLine(line);
            }
        }
        return false;
    }


    void transmitTime(TransactionBuilder builder) {
      long ts = System.currentTimeMillis();
      float tz = SimpleTimeZone.getDefault().getOffset(ts) / (1000 * 60 * 60.0f);
      // set time
      String cmd = "\u0010setTime("+(ts/1000)+");";
      // set timezone
      cmd += "E.setTimeZone("+tz+");";
      // write timezone to settings
      cmd += "(s=>{s&&(s.timezone="+tz+")&&require('Storage').write('setting.json',s);})(require('Storage').readJSON('setting.json',1))";
      uartTx(builder, cmd+"\n");
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }



    public String renderUnicodeAsImage(String txt) {
        if (txt==null) return null;
        // If we're not doing conversion, pass this right back
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (!devicePrefs.getBoolean(PREF_BANGLEJS_TEXT_BITMAP, false))
            return txt;
        // Otherwise split up and check each word
        String words[] = txt.split(" ");
        for (int i=0;i<words.length;i++) {
            boolean isRenderable = true;
            for (int j=0;j<words[i].length();j++) {
                char c = words[i].charAt(j);
                // TODO: better check?
                if (c<0 || c>255) isRenderable = false;
            }
            if (!isRenderable)
                words[i] = "\0"+bitmapToEspruinoString(textToBitmap(words[i]));
        }
        return String.join(" ", words);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "notify");
            o.put("id", notificationSpec.getId());
            o.put("src", notificationSpec.sourceName);
            o.put("title", renderUnicodeAsImage(notificationSpec.title));
            o.put("subject", renderUnicodeAsImage(notificationSpec.subject));
            o.put("body", renderUnicodeAsImage(notificationSpec.body));
            o.put("sender", renderUnicodeAsImage(notificationSpec.sender));
            o.put("tel", notificationSpec.phoneNumber);
            uartTxJSON("onNotification", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onDeleteNotification(int id) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "notify-");
            o.put("id", id);
            uartTxJSON("onDeleteNotification", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            transmitTime(builder);
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "alarm");
            JSONArray jsonalarms = new JSONArray();
            o.put("d", jsonalarms);

            for (Alarm alarm : alarms) {
                if (!alarm.getEnabled()) continue;
                JSONObject jsonalarm = new JSONObject();
                jsonalarms.put(jsonalarm);

                Calendar calendar = AlarmUtils.toCalendar(alarm);

                jsonalarm.put("h", alarm.getHour());
                jsonalarm.put("m", alarm.getMinute());
                jsonalarm.put("rep", alarm.getRepetition());
            }
            uartTxJSON("onSetAlarms", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "call");
            String cmdName = "";
            try {
                Field fields[] = callSpec.getClass().getDeclaredFields();
                for (Field field : fields)
                    if (field.getName().startsWith("CALL_") && field.getInt(callSpec) == callSpec.command)
                        cmdName = field.getName().substring(5).toLowerCase();
            } catch (IllegalAccessException e) {}
            o.put("cmd", cmdName);
            o.put("name", renderUnicodeAsImage(callSpec.name));
            o.put("number", callSpec.number);
            uartTxJSON("onSetCallState", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
}

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "musicstate");
            int musicState = stateSpec.state;
            String[] musicStates = {"play", "pause", "stop", ""};
            if (musicState<0) musicState=3;
            if (musicState>=musicStates.length) musicState = musicStates.length-1;
            o.put("state", musicStates[musicState]);
            o.put("position", stateSpec.position);
            o.put("shuffle", stateSpec.shuffle);
            o.put("repeat", stateSpec.repeat);
            uartTxJSON("onSetMusicState", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "musicinfo");
            o.put("artist", renderUnicodeAsImage(musicSpec.artist));
            o.put("album", renderUnicodeAsImage(musicSpec.album));
            o.put("track", renderUnicodeAsImage(musicSpec.track));
            o.put("dur", musicSpec.duration);
            o.put("c", musicSpec.trackCount);
            o.put("n", musicSpec.trackNr);
            uartTxJSON("onSetMusicInfo", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    private void transmitActivityStatus() {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "act");
            o.put("hrm", realtimeHRM);
            o.put("stp", realtimeStep);
            o.put("int", realtimeHRMInterval);
            uartTxJSON("onEnableRealtimeSteps", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        if (enable == realtimeHRM) return;
        realtimeStep = enable;
        transmitActivityStatus();
    }

    @Override
    public void onInstallApp(Uri uri) {

    }

    @Override
    public void onAppInfoReq() {

    }

    @Override
    public void onAppStart(UUID uuid, boolean start) {

    }

    @Override
    public void onAppDelete(UUID uuid) {

    }

    @Override
    public void onAppConfiguration(UUID appUuid, String config, Integer id) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchRecordedData(int dataTypes) {

    }

    @Override
    public void onReset(int flags) {

    }

    @Override
    public void onHeartRateTest() {

    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        if (enable == realtimeHRM) return;
        realtimeHRM = enable;
        transmitActivityStatus();
    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "find");
            o.put("n", start);
            uartTxJSON("onFindDevice", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "vibrate");
            o.put("n", integer);
            uartTxJSON("onSetConstantVibration", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {

    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        realtimeHRMInterval = seconds;
        transmitActivityStatus();
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "weather");
            o.put("temp", weatherSpec.currentTemp);
            o.put("hum", weatherSpec.currentHumidity);
            o.put("code", weatherSpec.currentConditionCode);
            o.put("txt", weatherSpec.currentCondition);
            o.put("wind", weatherSpec.windSpeed);
            o.put("wdir", weatherSpec.windDirection);
            o.put("loc", weatherSpec.location);
            uartTxJSON("onSendWeather", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    public Bitmap textToBitmap(String text) {
        Paint paint = new Paint(0); // Paint.ANTI_ALIAS_FLAG not wanted as 1bpp
        paint.setTextSize(18);
        paint.setColor(0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    /** Convert an Android bitmap to a base64 string for use in Espruino.
     * Currently only 1bpp, no scaling */
    public static byte[] bitmapToEspruinoArray(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        byte bmp[] = new byte[((height * width + 7) >> 3) + 3];
        int n = 0, c = 0, cn = 0;
        bmp[n++] = (byte)width;
        bmp[n++] = (byte)height;
        bmp[n++] = 1;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean pixel = (bitmap.getPixel(x, y) & 255) > 128;
                c = (c << 1) | (pixel?1:0);
                cn++;
                if (cn == 8) {
                    bmp[n++] = (byte)c;
                    cn = 0;
                    c = 0;
                }
            }
        }
        if (cn > 0) bmp[n++] = (byte)c;
        //LOG.info("BMP: " + width + "x"+height+" n "+n);
        // Convert to base64
        return bmp;
    }

    /** Convert an Android bitmap to a base64 string for use in Espruino.
     * Currently only 1bpp, no scaling */
    public static String bitmapToEspruinoString(Bitmap bitmap) {
        return new String(bitmapToEspruinoArray(bitmap), StandardCharsets.ISO_8859_1);
    }

    /** Convert an Android bitmap to a base64 string for use in Espruino.
     * Currently only 1bpp, no scaling */
    public static String bitmapToEspruinoBase64(Bitmap bitmap) {
        return Base64.encodeToString(bitmapToEspruinoArray(bitmap), Base64.DEFAULT).replaceAll("\n","");
    }

    /** Convert a drawable to a bitmap, for use with bitmapToEspruino */
    public static Bitmap drawableToBitmap(Drawable drawable) {
        final int maxWidth = 32;
        final int maxHeight = 32;
        /* Return bitmap directly but only if it's small enough. It could be
        we have a bitmap but it's just too big to send direct to the bangle */
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            Bitmap bmp = bitmapDrawable.getBitmap();
            if (bmp != null && bmp.getWidth()<=maxWidth && bmp.getHeight()<=maxHeight)
                return bmp;
        }
        /* Otherwise render this to a bitmap ourselves.. work out size */
        int w = maxWidth;
        int h = maxHeight;
        if (drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
            w = drawable.getIntrinsicWidth();
            h = drawable.getIntrinsicHeight();
            // don't allocate anything too big, but keep the ratio
            if (w>maxWidth) {
                h = h * maxWidth / w;
                w = maxWidth;
            }
            if (h>maxHeight) {
                w = w * maxHeight / h;
                h = maxHeight;
            }
        }
        /* render */
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
