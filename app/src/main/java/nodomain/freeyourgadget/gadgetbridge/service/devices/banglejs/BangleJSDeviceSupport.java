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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.UUID;
import java.lang.reflect.Field;

import io.wax911.emojify.Emoji;
import io.wax911.emojify.EmojiManager;
import io.wax911.emojify.EmojiUtils;
import de.greenrobot.dao.query.QueryBuilder;
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
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncState;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncStateDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarEvent;
import nodomain.freeyourgadget.gadgetbridge.util.calendar.CalendarManager;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.EmojiConverter;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
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
    int bangleCommandSeq = 0; // to attempt to stop duplicate packets when sending Local Intents

    /// Current line of data received from Bangle.js
    private String receivedLine = "";
    /// All characters received from Bangle.js for debug purposes (limited to MAX_RECEIVE_HISTORY_CHARS). Can be dumped with 'Fetch Device Debug Logs' from Debug menu
    private String receiveHistory = "";
    private boolean realtimeHRM = false;
    private boolean realtimeStep = false;
    private int realtimeHRMInterval = 30*60;

    private final LimitedQueue/*Long*/ mNotificationReplyAction = new LimitedQueue(16);

    /// Maximum amount of characters to store in receiveHistory
    public static final int MAX_RECEIVE_HISTORY_CHARS = 100000;

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

    private void addReceiveHistory(String s) {
        receiveHistory += s;
        if (receiveHistory.length() > MAX_RECEIVE_HISTORY_CHARS)
            receiveHistory = receiveHistory.substring(receiveHistory.length() - MAX_RECEIVE_HISTORY_CHARS);
    }

    private void registerLocalIntents() {
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(GBDevice.ACTION_DEVICE_CHANGED);
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
                    case GBDevice.ACTION_DEVICE_CHANGED: {
                        LOG.info("ACTION_DEVICE_CHANGED " + gbDevice.getStateString());
                        addReceiveHistory("\n================================================\nACTION_DEVICE_CHANGED "+gbDevice.getStateString()+" "+(new SimpleDateFormat("yyyy-mm-dd hh:mm:ss")).format(Calendar.getInstance().getTime())+"\n================================================\n");
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
        byte[] bytes = str.getBytes(StandardCharsets.ISO_8859_1);
        LOG.info("UART TX: " + str);
        addReceiveHistory("\n================================================\nSENDING "+str+"\n================================================\n");
        // FIXME: somehow this is still giving us UTF8 data when we put images in strings. Maybe JSON.stringify is converting to UTF-8?
        for (int i=0;i<bytes.length;i+=mtuSize) {
            int l = bytes.length-i;
            if (l>mtuSize) l=mtuSize;
            byte[] packet = new byte[l];
            System.arraycopy(bytes, i, packet, 0, l);
            builder.write(txCharacteristic, packet);
        }
    }

    /// Converts an object to a JSON string. see jsonToString
    private String jsonToStringInternal(Object v) {
        if (v instanceof String) {
            /* Convert a string, escaping chars we can't send over out UART connection */
            String s = (String)v;
            String json = "\"";
            //String rawString = "";
            for (int i=0;i<s.length();i++) {
                int ch = (int)s.charAt(i); // 0..255
                int nextCh = (int)(i+1<s.length() ? s.charAt(i+1) : 0); // 0..255
                //rawString = rawString+ch+",";
                if (ch<8) {
                    // if the next character is a digit, it'd be interpreted
                    // as a 2 digit octal character, so we can't use `\0` to escape it
                    if (nextCh>='0' && nextCh<='7') json += "\\x0" + ch;
                    else json += "\\" + ch;
                } else if (ch==8) json += "\\b";
                else if (ch==9) json += "\\t";
                else if (ch==10) json += "\\n";
                else if (ch==11) json += "\\v";
                else if (ch==12) json += "\\f";
                else if (ch==34) json += "\\\""; // quote
                else if (ch==92) json += "\\\\"; // slash
                else if (ch<32 || ch==127 || ch==173)
                    json += "\\x"+Integer.toHexString((ch&255)|256).substring(1);
                else json += s.charAt(i);
            }
            // if it was less characters to send base64, do that!
            if (json.length() > 5+(s.length()*4/3)) {
                byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
                return "atob(\""+Base64.encodeToString(bytes, Base64.DEFAULT).replaceAll("\n","")+"\")";
            }
            // for debugging...
            //addReceiveHistory("\n---------------------\n"+rawString+"\n---------------------\n");
            return json + "\"";
        } else if (v instanceof JSONArray) {
            JSONArray a = (JSONArray)v;
            String json = "[";
            for (int i=0;i<a.length();i++) {
                if (i>0) json += ",";
                Object o = null;
                try {
                    o = a.get(i);
                } catch (JSONException e) {
                    LOG.warn("jsonToString array error: " + e.getLocalizedMessage());
                }
                json += jsonToStringInternal(o);
            }
            return json+"]";
        } else if (v instanceof JSONObject) {
            JSONObject obj = (JSONObject)v;
            String json = "{";
            Iterator<String> iter = obj.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object o = null;
                try {
                    o = obj.get(key);
                } catch (JSONException e) {
                    LOG.warn("jsonToString object error: " + e.getLocalizedMessage());
                }
                json += key+":"+jsonToStringInternal(o);
                if (iter.hasNext()) json+=",";
            }
            return json+"}";
        } // else int/double/null
        return v.toString();
    }

    /// Convert a JSON object to a JSON String (NOT 100% JSON compliant)
    public String jsonToString(JSONObject jsonObj) {
        /* jsonObj.toString() works but breaks char codes>128 (encodes as UTF8?) and also uses
        \u0000 when just \0 would do (and so on).

        So we do it manually, which can be more compact anyway.
        This is JSON-ish, so not exactly as per JSON1 spec but good enough for Espruino.
        */
        return jsonToStringInternal(jsonObj);
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
        if (line.length()==0) return;
        if (">Uncaught ReferenceError: \"GB\" is not defined".equals(line))
          GB.toast(getContext(), "Gadgetbridge plugin not installed on Bangle.js", Toast.LENGTH_LONG, GB.ERROR);
        else if (line.charAt(0)=='{') {
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
                /* REPLY responses don't use the ID from the event (MUTE/etc seem to), but instead
                * they use a handle that was provided in an action list on the onNotification.. event  */
                if (deviceEvtNotificationControl.event == GBDeviceEventNotificationControl.Event.REPLY) {
                    Long foundHandle = (Long)mNotificationReplyAction.lookup((int)deviceEvtNotificationControl.handle);
                    if (foundHandle!=null)
                        deviceEvtNotificationControl.handle = foundHandle;
                }
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
            } break;
            case "force_calendar_sync": {
                //if(!GBApplication.getPrefs().getBoolean("enable_calendar_sync", false)) return;
                //pretty much like the updateEvents in CalendarReceiver, but would need a lot of libraries here
                JSONArray ids = json.getJSONArray("ids");
                ArrayList<Long> idsList = new ArrayList(ids.length());
                try (DBHandler dbHandler = GBApplication.acquireDB()) {
                    DaoSession session = dbHandler.getDaoSession();
                    Long deviceId = DBHelper.getDevice(gbDevice, session).getId();
                    QueryBuilder<CalendarSyncState> qb = session.getCalendarSyncStateDao().queryBuilder();
                    //FIXME just use that and don't query every time?
                    List<CalendarSyncState> states = qb.where(
                                CalendarSyncStateDao.Properties.DeviceId.eq(deviceId)).build().list();

                    LOG.info("force_calendar_sync on banglejs: "+ ids.length() +" events on the device, "+ states.size() +" on our db");
                    for (int i = 0; i < ids.length(); i++) {
                        Long id = ids.getLong(i);
                        qb = session.getCalendarSyncStateDao().queryBuilder(); //is this needed again?
                        CalendarSyncState calendarSyncState = qb.where(
                                qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId),
                                    CalendarSyncStateDao.Properties.CalendarEntryId.eq(id))).build().unique();
                        if(calendarSyncState == null) {
                            onDeleteCalendarEvent((byte)0, id);
                            LOG.info("event id="+ id +" is on device id="+ deviceId +", removing it there");
                        } else {
                            //used for later, no need to check twice the ones that do not match
                            idsList.add(id);
                        }
                    }
                    if(idsList.size() == states.size()) return;
                    //remove all elements not in ids from database (we don't have them)
                    for(CalendarSyncState calendarSyncState : states) {
                        long id = calendarSyncState.getCalendarEntryId();
                        if(!idsList.contains(id)) {
                            qb = session.getCalendarSyncStateDao().queryBuilder(); //is this needed again?
                            qb.where(qb.and(CalendarSyncStateDao.Properties.DeviceId.eq(deviceId),
                                        CalendarSyncStateDao.Properties.CalendarEntryId.eq(id)))
                                .buildDelete().executeDeleteWithoutDetachingEntities();
                            LOG.info("event id="+ id +" is not on device id="+ deviceId +", removing from our db");
                        }
                    }
                } catch (Exception e1) {
                    GB.toast("Database Error while forcefully syncing Calendar", Toast.LENGTH_SHORT, GB.ERROR, e1);
                }
                //force a syncCalendar now, send missing events
                Intent in = new Intent(this.getContext().getApplicationContext(), CalendarReceiver.class);
                in.setAction("android.intent.action.PROVIDER_CHANGED");
                this.getContext().getApplicationContext().sendBroadcast(in);
            } break;
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
            // logging
            addReceiveHistory(packetStr);
            // split into input lines
            receivedLine += packetStr;
            while (receivedLine.contains("\n")) {
                int p = receivedLine.indexOf("\n");
                String line =  receivedLine.substring(0,p-1);
                receivedLine = receivedLine.substring(p+1);
                handleUartRxLine(line);
            }
            // Send an intent with new data
            Intent intent = new Intent(BangleJSDeviceSupport.BANGLEJS_COMMAND_RX);
            intent.putExtra("DATA", packetStr);
            intent.putExtra("SEQ", bangleCommandSeq++);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
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

    private String renderUnicodeWordAsImage(String word) {
        // check for emoji
        boolean hasEmoji = false;
        if (EmojiUtils.getAllEmojis()==null)
            EmojiManager.initEmojiData(GBApplication.getContext());
        for(Emoji emoji : EmojiUtils.getAllEmojis())
            if (word.contains(emoji.getEmoji())) hasEmoji = true;
        // if we had emoji, ensure we create 3 bit color (not 1 bit B&W)
        return "\0"+bitmapToEspruinoString(textToBitmap(word), hasEmoji ? BangleJSBitmapStyle.RGB_3BPP : BangleJSBitmapStyle.MONOCHROME);
    }

    public String renderUnicodeAsImage(String txt) {
        if (txt==null) return null;
        // Simple conversions
        txt = txt.replaceAll("…", "...");
        /* If we're not doing conversion, pass this right back (we use the EmojiConverter
        As we would have done if BangleJSCoordinator.supportsUnicodeEmojis had reported false */
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (!devicePrefs.getBoolean(PREF_BANGLEJS_TEXT_BITMAP, false))
            return EmojiConverter.convertUnicodeEmojiToAscii(txt, GBApplication.getContext());
         // Otherwise split up and check each word
        String word = "", result = "";
        boolean needsTranslate = false;
        for (int i=0;i<txt.length();i++) {
            char ch = txt.charAt(i);
            if (" -_/:.,?!'\"&*()".indexOf(ch)>=0) {
                // word split
                if (needsTranslate) { // convert word
                    result += renderUnicodeWordAsImage(word)+ch;
                } else { // or just copy across
                    result += word+ch;
                }
                word = "";
                needsTranslate = false;
            } else {
                // TODO: better check?
                if (ch<0 || ch>255) needsTranslate = true;
                word += ch;
            }
        }
        if (needsTranslate) { // convert word
            result += renderUnicodeWordAsImage(word);
        } else { // or just copy across
            result += word;
        }
        return result;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (notificationSpec.attachedActions!=null)
            for (int i=0;i<notificationSpec.attachedActions.size();i++) {
                NotificationSpec.Action action = notificationSpec.attachedActions.get(i);
                if (action.type==NotificationSpec.Action.TYPE_WEARABLE_REPLY)
                    mNotificationReplyAction.add(notificationSpec.getId(), new Long(((long)notificationSpec.getId()<<4) + i + 1)); // wow. This should be easier!
            }
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
            //TODO: once we have a common strategy for sending events (e.g. EventHandler), remove this call from here. Meanwhile it does no harm.
            // = we should genaralize the pebble calender code
            //sendCalendarEvents(builder);
            forceCalendarSync();
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
        if (dataTypes == RecordedDataTypes.TYPE_DEBUGLOGS) {
            File dir;
            try {
                dir = FileUtils.getExternalFilesDir();
            } catch (IOException e) {
                return;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
            String filename = "banglejs_debug_" + dateFormat.format(new Date()) + ".log";
            File outputFile = new File(dir, filename );
            LOG.warn("Writing log to "+outputFile.toString());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                writer.write(receiveHistory);
                writer.close();
                receiveHistory = "";
                GB.toast(getContext(), "Log written to "+filename, Toast.LENGTH_LONG, GB.INFO);
            } catch (IOException e) {
                LOG.warn("Could not write to file", e);
                return;
            }
        }
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
        try {
            JSONObject o = new JSONObject();
            o.put("t", "calendar"); //TODO implement command
            o.put("id", calendarEventSpec.id);
            o.put("type", calendarEventSpec.type); //implement this too? (sunrise and set)
            o.put("timestamp", calendarEventSpec.timestamp);
            o.put("durationInSeconds", calendarEventSpec.durationInSeconds);
            o.put("title", calendarEventSpec.title);
            o.put("description", calendarEventSpec.description);
            o.put("location", calendarEventSpec.location);
            o.put("allDay", calendarEventSpec.allDay);
            uartTxJSON("onAddCalendarEvent", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "calendar-");
            o.put("id", id);
            uartTxJSON("onDeleteCalendarEvent", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
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

    public enum BangleJSBitmapStyle {
        MONOCHROME,
        RGB_3BPP
    };

    /** Used for writing single bits to an array */
    public static class BitWriter {
        int n;
        byte[] bits;
        int currentByte, bitIdx;

        public BitWriter(byte[] array, int offset) {
            bits = array;
            n = offset;
        }

        public void push(boolean v) {
            currentByte = (currentByte << 1) | (v?1:0);
            bitIdx++;
            if (bitIdx == 8) {
                bits[n++] = (byte)currentByte;
                bitIdx = 0;
                currentByte = 0;
            }
        }

        public void finish() {
            if (bitIdx > 0) bits[n++] = (byte)currentByte;
        }
    }

    /** Convert an Android bitmap to a base64 string for use in Espruino.
     * Currently only 1bpp, no scaling */
    public static byte[] bitmapToEspruinoArray(Bitmap bitmap, BangleJSBitmapStyle style) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int bpp = (style==BangleJSBitmapStyle.RGB_3BPP) ? 3 : 1;
        byte pixels[] = new byte[width * height];
        final byte PIXELCOL_TRANSPARENT = -1;
        final int ditherMatrix[] = {1*16,5*16,7*16,3*16}; // for bayer dithering
        // if doing 3bpp, check image to see if it's transparent
        boolean allowTransparency = (style != BangleJSBitmapStyle.MONOCHROME);
        boolean isTransparent = false;
        byte transparentColorIndex = 0;
        /* Work out what colour index each pixel should be and write to pixels.
         Also figure out if we're transparent at all, and how often each color is used */
        int colUsage[] = new int[8];
        int n = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int r = pixel & 255;
                int g = (pixel >> 8) & 255;
                int b = (pixel >> 16) & 255;
                int a = (pixel >> 24) & 255;
                boolean pixelTransparent = allowTransparency && (a < 128);
                if (pixelTransparent) {
                    isTransparent = true;
                    r = g = b = 0;
                }
                // do dithering here
                int ditherAmt = ditherMatrix[(x&1) + (y&1)*2];
                r += ditherAmt;
                g += ditherAmt;
                b += ditherAmt;
                int col = 0;
                if (style == BangleJSBitmapStyle.MONOCHROME)
                    col = ((r+g+b) >= 768)?1:0;
                else if (style == BangleJSBitmapStyle.RGB_3BPP)
                    col = ((r>=256)?1:0) | ((g>=256)?2:0) | ((b>=256)?4:0);
                if (!pixelTransparent) colUsage[col]++; // if not transparent, record usage
                // save colour, mark transparent separately
                pixels[n++] = (byte)(pixelTransparent ? PIXELCOL_TRANSPARENT : col);
            }
        }
        // if we're transparent, find the least-used color, and use that for transparency
        if (isTransparent) {
            // find least used
            int minColUsage = -1;
            for (int c=0;c<8;c++) {
                if (minColUsage<0 || colUsage[c]<minColUsage) {
                    minColUsage = colUsage[c];
                    transparentColorIndex = (byte)c;
                }
            }
            // rewrite any transparent pixels as the correct color for transparency
            for (n=0;n<pixels.length;n++)
                if (pixels[n]==PIXELCOL_TRANSPARENT)
                    pixels[n] = transparentColorIndex;
        }
        // Write the header
        int headerLen = isTransparent ? 4 : 3;
        byte bmp[] = new byte[(((height * width * bpp) + 7) >> 3) + headerLen];
        bmp[0] = (byte)width;
        bmp[1] = (byte)height;
        bmp[2] = (byte)(bpp + (isTransparent?128:0));
        if (isTransparent) bmp[3] = transparentColorIndex;
        // Now write the image out bit by bit
        BitWriter bits = new BitWriter(bmp, headerLen);
        n = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = pixels[n++];
                for (int b=bpp-1;b>=0;b--)
                    bits.push(((pixel>>b)&1) != 0);
            }
        }
        return bmp;
    }

    /** Convert an Android bitmap to a base64 string for use in Espruino.
     * Currently only 1bpp, no scaling */
    public static String bitmapToEspruinoString(Bitmap bitmap, BangleJSBitmapStyle style) {
        return new String(bitmapToEspruinoArray(bitmap, style), StandardCharsets.ISO_8859_1);
    }

    /** Convert an Android bitmap to a base64 string for use in Espruino.
     * Currently only 1bpp, no scaling */
    public static String bitmapToEspruinoBase64(Bitmap bitmap, BangleJSBitmapStyle style) {
        return Base64.encodeToString(bitmapToEspruinoArray(bitmap, style), Base64.DEFAULT).replaceAll("\n","");
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

    /*
     * Request the banglejs to send all ids to sync with our database
     * TODO perhaps implement a minimum interval between consecutive requests
     */
    private void forceCalendarSync() {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "force_calendar_sync_start");
            uartTxJSON("forceCalendarSync", o);
        } catch(JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    /*
     * Sending all events together, not used for now, keep for future reference
     */
    private void sendCalendarEvents(TransactionBuilder builder) {
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        //TODO set a limit as number in the preferences?
        //int availableSlots = prefs.getInt(PREF_CALENDAR_EVENTS_MAX, 0);
        int availableSlots = 6;

        try {
            CalendarManager upcomingEvents = new CalendarManager(getContext(), getDevice().getAddress());
            List<CalendarEvent> mEvents = upcomingEvents.getCalendarEventList();
            JSONObject cal = new JSONObject();
            JSONArray events = new JSONArray();

            cal.put("t", "calendarevents");

            for (CalendarEvent mEvt : mEvents) {
                if(availableSlots<1) break;
                JSONObject o = new JSONObject();
                o.put("timestamp", mEvt.getBeginSeconds());
                o.put("durationInSeconds", mEvt.getDurationSeconds());
                o.put("title", mEvt.getTitle());
                //avoid making the message too long
                //o.put("description", mEvt.getDescription());
                o.put("location", mEvt.getLocation());
                o.put("allDay", mEvt.isAllDay());
                events.put(o);
                availableSlots--;
            }
            cal.put("events", events);
            uartTxJSON("sendCalendarEvents", cal);
            LOG.info("sendCalendarEvents: sent " + events.length());
        } catch(JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }
}
