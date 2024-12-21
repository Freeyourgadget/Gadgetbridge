/*  Copyright (C) 2019-2024 Albert, Andreas Shimokawa, Arjan Schrijver, Damien
    Gaignon, Gabriele Monaco, Ganblejs, gfwilliams, glemco, Gordon Williams,
    halemmerich, illis, José Rebelo, Lukas, LukasEdl, Marc Nause, Martin Boonk,
    rarder44, Richard de Boer, Simon Sievert

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_ALLOW_HIGH_MTU;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANGLEJS_TEXT_BITMAP;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_BANGLEJS_TEXT_BITMAP_SIZE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_GPS_UPDATE;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_GPS_UPDATE_INTERVAL;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_GPS_USE_NETWORK_ONLY;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTENTS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_DEVICE_INTERNET_ACCESS;
import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.PREF_NOTIFICATION_WAKE_ON_OPEN;
import static nodomain.freeyourgadget.gadgetbridge.database.DBHelper.getUser;
import static nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants.PREF_BANGLEJS_ACTIVITY_FULL_SYNC_START;
import static nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants.PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STATUS;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import de.greenrobot.dao.query.QueryBuilder;
import io.wax911.emojify.EmojiManager;
import io.wax911.emojify.parser.EmojiParserKt;
import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.WakeActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.BarcodeFormat;
import nodomain.freeyourgadget.gadgetbridge.capabilities.loyaltycards.LoyaltyCard;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventNotificationControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventScreenshot;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventUpdatePreferences;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BangleJSActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncState;
import nodomain.freeyourgadget.gadgetbridge.entities.CalendarSyncStateDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.externalevents.CalendarReceiver;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationProviderType;
import nodomain.freeyourgadget.gadgetbridge.externalevents.gps.GBLocationService;
import nodomain.freeyourgadget.gadgetbridge.externalevents.sleepasandroid.SleepAsAndroidAction;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NavigationInfoSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.SleepAsAndroidSender;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.util.EmojiConverter;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

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
    /// How often should activity data be sent - in seconds
    private int realtimeHRMInterval = 10;
    /// Last battery percentage reported (or -1) to help with smoothing reported battery levels
    private int lastBatteryPercent = -1;

    private final LimitedQueue<Integer, Long> mNotificationReplyAction = new LimitedQueue<>(16);

    private boolean gpsUpdateSetup = false;

    // this stores the globalUartReceiver (for uart.tx intents)
    private BroadcastReceiver globalUartReceiver = null;

    // used to make HTTP requests and handle responses
    private RequestQueue requestQueue = null;

    /// Maximum amount of characters to store in receiveHistory
    public static final int MAX_RECEIVE_HISTORY_CHARS = 100000;
    /// Used to avoid spamming logs with ACTION_DEVICE_CHANGED messages
    static String lastStateString;

    // Local Intents - for app manager communication
    public static final String BANGLEJS_COMMAND_TX = "banglejs_command_tx";
    public static final String BANGLEJS_COMMAND_RX = "banglejs_command_rx";
    // Global Intents
    private static final String BANGLE_ACTION_UART_TX = "com.banglejs.uart.tx";

    private SleepAsAndroidSender sleepAsAndroidSender;

    public BangleJSDeviceSupport() {
        super(LOG);
        addSupportedService(BangleJSConstants.UUID_SERVICE_NORDIC_UART);

        registerLocalIntents();
        registerGlobalIntents();
    }

    @Override
    public void dispose() {
        super.dispose();
        stopGlobalUartReceiver();
        stopLocationUpdate();
        stopRequestQueue();
    }

    private void stopGlobalUartReceiver(){
        if(globalUartReceiver != null){
            GBApplication.getContext().unregisterReceiver(globalUartReceiver); // remove uart.tx intent listener
        }
    }


    private void stopLocationUpdate() {
        if (!gpsUpdateSetup)
            return;
        LOG.info("Stop location updates");
        GBLocationService.stop(getContext(), getDevice());
        gpsUpdateSetup = false;
    }

    private void stopRequestQueue() {
        if (requestQueue != null) {
            requestQueue.stop();
        }
    }

    private RequestQueue getRequestQueue() {
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(getContext());
        }
        return requestQueue;
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
                        BtLEQueue queue = getQueue();
                        if (queue==null) {
                            LOG.warn("BANGLEJS_COMMAND_TX received, but getQueue()==null (state=" + gbDevice.getStateString(context) + ")");
                        } else {
                            try {
                                TransactionBuilder builder = performInitialized("TX");
                                uartTx(builder, data);
                                builder.queue(queue);
                            } catch (IOException e) {
                                GB.toast(getContext(), "Error in TX: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                            }
                        }
                        break;
                    }
                    case GBDevice.ACTION_DEVICE_CHANGED: {
                        String stateString = (gbDevice!=null ? gbDevice.getStateString(context):"");
                        if (!stateString.equals(lastStateString)) {
                          lastStateString = stateString;
                          LOG.info("ACTION_DEVICE_CHANGED " + stateString);
                          addReceiveHistory("\n================================================\nACTION_DEVICE_CHANGED "+stateString+" "+(new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US)).format(Calendar.getInstance().getTime())+"\n================================================\n");
                        }
                        if (gbDevice!=null && (gbDevice.getState() == GBDevice.State.NOT_CONNECTED || gbDevice.getState() == GBDevice.State.WAITING_FOR_RECONNECT)) {
                            stopLocationUpdate();
                        }
                    }
                }
            }
        };
        LocalBroadcastManager.getInstance(GBApplication.getContext()).registerReceiver(commandReceiver, commandFilter);
    }

    private void registerGlobalIntents() {
        IntentFilter commandFilter = new IntentFilter();
        commandFilter.addAction(BANGLE_ACTION_UART_TX);
        globalUartReceiver = new BroadcastReceiver() {
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

        ContextCompat.registerReceiver(GBApplication.getContext(), globalUartReceiver, commandFilter, ContextCompat.RECEIVER_EXPORTED);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        if (sleepAsAndroidSender == null) {
            sleepAsAndroidSender = new SleepAsAndroidSender(gbDevice);
        }

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        rxCharacteristic = getCharacteristic(BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX);
        txCharacteristic = getCharacteristic(BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_TX);
        if (rxCharacteristic==null || txCharacteristic==null) {
            // https://codeberg.org/Freeyourgadget/Gadgetbridge/issues/2996 - sometimes we get
            // initializeDevice called but no characteristics have been fetched - try and reconnect in that case
            LOG.warn("RX/TX characteristics are null, will attempt to reconnect");
            builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.WAITING_FOR_RECONNECT, getContext()));
        }
        builder.setCallback(this);
        builder.notify(rxCharacteristic, true);

        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        allowHighMTU = devicePrefs.getBoolean(PREF_ALLOW_HIGH_MTU, true);

        if (allowHighMTU) {
            builder.requestMtu(131);
        }
        // No need to clear active line with Ctrl-C now - firmwares in 2023 auto-clear on connect

        Prefs prefs = GBApplication.getPrefs();
        if (prefs.getBoolean("datetime_synconconnect", true))
          transmitTime(builder);
        //sendSettings(builder);

        // get version
        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());
        if (getDevice().getFirmwareVersion() == null) {
            getDevice().setFirmwareVersion("N/A");
            getDevice().setFirmwareVersion2("N/A");
        }
        lastBatteryPercent = -1;

        LOG.info("Initialization Done");

        requestBangleGPSPowerStatus();

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
            StringBuilder json = new StringBuilder("\"");
            boolean hasUnicode = false;
            //String rawString = "";
            for (int i=0;i<s.length();i++) {
                int ch = (int)s.charAt(i); // unicode, so 0..65535 (usually)
                int nextCh = (int)(i+1<s.length() ? s.charAt(i+1) : 0); // 0..65535
                //rawString = rawString+ch+",";
                if (ch>255) hasUnicode = true;
                if (ch<8) {
                    // if the next character is a digit, it'd be interpreted
                    // as a 2 digit octal character, so we can't use `\0` to escape it
                    if (nextCh>='0' && nextCh<='7') json.append("\\x0").append(ch);
                    else json.append("\\").append(ch);
                } else if (ch==8) json.append("\\b");
                else if (ch==9) json.append("\\t");
                else if (ch==10) json.append("\\n");
                else if (ch==11) json.append("\\v");
                else if (ch==12) json.append("\\f");
                else if (ch==16) json.append("\\20"); // DLE - not entirely safe to use
                else if (ch==34) json.append("\\\""); // quote
                else if (ch==92) json.append("\\\\"); // slash
                else if (ch<32 || ch==127 || ch==173 ||
                         ((ch>=0xC2) && (ch<=0xF4))) // unicode start char range
                    json.append("\\x").append(Integer.toHexString((ch & 255) | 256).substring(1));
                else if (ch>255)
                    json.append("\\u").append(Integer.toHexString((ch & 65535) | 65536).substring(1));
                else json.append(s.charAt(i));
            }
            // if it was less characters to send base64, do that!
            if (!hasUnicode && (json.length() > 5+(s.length()*4/3))) {
                byte[] bytes = s.getBytes(StandardCharsets.ISO_8859_1);
                return "atob(\""+Base64.encodeToString(bytes, Base64.DEFAULT).replaceAll("\n","")+"\")";
            }
            // for debugging...
            //addReceiveHistory("\n---------------------\n"+rawString+"\n---------------------\n");
            return json.append("\"").toString();
        } else if (v instanceof JSONArray) {
            JSONArray a = (JSONArray)v;
            StringBuilder json = new StringBuilder("[");
            for (int i=0;i<a.length();i++) {
                if (i>0) json.append(",");
                Object o = null;
                try {
                    o = a.get(i);
                } catch (JSONException e) {
                    LOG.warn("jsonToString array error: " + e.getLocalizedMessage());
                }
                json.append(jsonToStringInternal(o));
            }
            return json.append("]").toString();
        } else if (v instanceof JSONObject) {
            JSONObject obj = (JSONObject)v;
            StringBuilder json = new StringBuilder("{");
            Iterator<String> iter = obj.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                Object o = null;
                try {
                    o = obj.get(key);
                } catch (JSONException e) {
                    LOG.warn("jsonToString object error: " + e.getLocalizedMessage());
                }
                json.append("\"").append(key).append("\":").append(jsonToStringInternal(o));
                if (iter.hasNext()) json.append(",");
            }
            return json.append("}").toString();
        } else if (v==null) {
            // else int/double/null
            return "null";
        }
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

    private void uartTxJSONError(String taskName, String message, String id) {
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
          GB.toast(getContext(), "'Android Integration' plugin not installed on Bangle.js", Toast.LENGTH_LONG, GB.ERROR);
        else if (line.charAt(0)=='{') {
            // JSON - we hope!
            try {
                JSONObject json = new JSONObject(line);
                if (json.has("t")) {
                    handleUartRxJSON(json);
                    LOG.info("UART RX JSON parsed successfully");
                } else
                    LOG.warn("UART RX JSON parsed but doesn't contain 't' - ignoring");
            } catch (JSONException e) {
                LOG.error("UART RX JSON parse failure: "+ e.getLocalizedMessage());
                GB.toast(getContext(), "Malformed JSON from Bangle.js: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
            }
        } else if (line.startsWith("data:image/bmp;base64,")) {
            LOG.debug("Got screenshot bmp");
            final byte[] screenshotBytes = Base64.decode(line.substring(21), Base64.DEFAULT);
            final GBDeviceEventScreenshot gbDeviceEventScreenshot = new GBDeviceEventScreenshot(screenshotBytes);
            evaluateGBDeviceEvent(gbDeviceEventScreenshot);
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
                final GBDeviceEventVersionInfo gbDeviceEventVersionInfo = new GBDeviceEventVersionInfo();
                if (json.has("fw"))
                    gbDeviceEventVersionInfo.fwVersion = json.getString("fw");
                if (json.has("hw"))
                    gbDeviceEventVersionInfo.hwVersion = json.getString("hw");
                evaluateGBDeviceEvent(gbDeviceEventVersionInfo);
            } break;
            case "findPhone": {
                boolean start = json.has("n") && json.getBoolean("n");
                GBDeviceEventFindPhone deviceEventFindPhone = new GBDeviceEventFindPhone();
                deviceEventFindPhone.event = start ? GBDeviceEventFindPhone.Event.START : GBDeviceEventFindPhone.Event.STOP;
                evaluateGBDeviceEvent(deviceEventFindPhone);
            } break;
            case "music": {
                GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
                deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.valueOf(json.getString("n").toUpperCase(Locale.US));
                evaluateGBDeviceEvent(deviceEventMusicControl);
            } break;
            case "call": {
                GBDeviceEventCallControl deviceEventCallControl = new GBDeviceEventCallControl();
                deviceEventCallControl.event = GBDeviceEventCallControl.Event.valueOf(json.getString("n").toUpperCase(Locale.US));
                evaluateGBDeviceEvent(deviceEventCallControl);
            } break;
            case "status":
                handleBatteryStatus(json);
                break;
            case "notify" :
                handleNotificationControl(json);
                break;
            case "actfetch":
                handleActivityFetch(json);
                break;
            case "act":
                handleActivity(json);
                break;
            case "actTrksList": {
                JSONObject requestTrackObj = BangleJSActivityTrack.handleActTrksList(json, getDevice(), getContext());
                if (requestTrackObj!=null) uartTxJSON("requestActivityTrackLog", requestTrackObj);
            } break;
            case "actTrk": {
                JSONObject requestTrackObj = BangleJSActivityTrack.handleActTrk(json, getDevice(), getContext());
                if (requestTrackObj!=null) uartTxJSON("requestActivityTrackLog", requestTrackObj);
            } break;
            case "http":
                handleHttp(json);
                break;
            case "force_calendar_sync":
                handleCalendarSync(json);
                break;
            case "intent":
                handleIntent(json);
                break;
            case "file":
                handleFile(json);
                break;
            case "gps_power": {
                boolean status = json.getBoolean("status");
                LOG.info("Got gps power status: " + status);
                if (status) {
                    setupGPSUpdateTimer();
                } else {
                    stopLocationUpdate();
                }
            } break;
            case "accel":
                handleAcceleration(json);
                break;
            default : {
                LOG.info("UART RX JSON packet type '"+packetType+"' not understood.");
            }
        }
    }

    @Override
    public void onSleepAsAndroidAction(String action, Bundle extras) {
        // Validate if our device can work with an action
        try {
            sleepAsAndroidSender.validateAction(action);
        } catch (UnsupportedOperationException e) {
            return;
        }

        // Consult the SleepAsAndroid documentation for a set of actions and their extra
        // https://docs.sleep.urbandroid.org/devs/wearable_api.html
        switch (action) {
            case SleepAsAndroidAction.CHECK_CONNECTED:
                sleepAsAndroidSender.confirmConnected();
                break;
            // Received when the app starts sleep tracking
            case SleepAsAndroidAction.START_TRACKING:
                this.enableAccelSender(true);
                sleepAsAndroidSender.startTracking();
                break;
            // Received when the app stops sleep tracking
            case SleepAsAndroidAction.STOP_TRACKING:
                this.enableAccelSender(false);
                sleepAsAndroidSender.stopTracking();
                break;
            case SleepAsAndroidAction.SET_SUSPENDED:
                boolean suspended = extras.getBoolean("SUSPENDED", false);
                this.enableAccelSender(false);
                sleepAsAndroidSender.pauseTracking(suspended);
                break;
                // Received when the app changes the batch size for the movement data
            case SleepAsAndroidAction.SET_BATCH_SIZE:
                long batchSize = extras.getLong("SIZE", 12L);
                sleepAsAndroidSender.setBatchSize(batchSize);
                break;
            // Received when the app sends a notificaation
            case SleepAsAndroidAction.SHOW_NOTIFICATION:
                NotificationSpec notificationSpec = new NotificationSpec();
                notificationSpec.title = extras.getString("TITLE");
                notificationSpec.body = extras.getString("BODY");
                this.onNotification(notificationSpec);
                break;
            case SleepAsAndroidAction.UPDATE_ALARM:
                long alarmTimestamp = extras.getLong("TIMESTAMP");

                // Sets the alarm at a giver hour and minute
                // Snoozing from the app will create a new alarm in the future
                this.setSleepAsAndroidAlarm(alarmTimestamp);
                break;
            default:
                LOG.warn("Received unsupported " + action);
                break;
        }
    }

    private void enableAccelSender(boolean enable) {
        /**
         * Sends an event to the Banglejs to enable/disable Acceleration tracking
         * @param enable: whether to enable tracking
         **/
        try {
            JSONObject o = new JSONObject();
            o.put("t", "accelsender");
            o.put("enable", enable);
            o.put("interval", 1000);
            uartTxJSON("enableAccelSender", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    private void setSleepAsAndroidAlarm(long alarmTimestamp) {
        /**
         * Updates the Sleep as Android Alarm slot.
         * @param alarmTimestamp: Unix timestamp of the upcoming alarm.
         **/
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(new Timestamp(alarmTimestamp).getTime());

        // Get Alarm in relevant slot
        nodomain.freeyourgadget.gadgetbridge.entities.Alarm currentAlarm = DBHelper.getAlarms(gbDevice).get(SleepAsAndroidSender.getAlarmSlot());
        currentAlarm.setRepetition(Alarm.ALARM_ONCE);
        currentAlarm.setHour(calendar.get(Calendar.HOUR_OF_DAY));
        currentAlarm.setMinute(calendar.get(Calendar.MINUTE));
        currentAlarm.setEnabled(true);
        currentAlarm.setUnused(false);

        // Store modified alarm
        DBHelper.store(currentAlarm);

        // Send alarms to Gadgetbridge
        this.onSetAlarms(new ArrayList<Alarm>(DBHelper.getAlarms(gbDevice)));

    }

    /**
     * Handle "accel" packets: Acceleration data streaming
     */
    private void handleAcceleration(JSONObject json) throws JSONException {
        if (json.has("accel")) {
            JSONObject accel = json.getJSONObject("accel");
            sleepAsAndroidSender.onAccelChanged((float) (accel.getDouble("x") * 9.80665),
                    (float) (accel.getDouble("y") * 9.80665), (float) (accel.getDouble("z") * 9.80665));
        }
    }

    /**
     * Handle "status" packets: battery info updates
     */
    private void handleBatteryStatus(JSONObject json) throws JSONException {
        GBDeviceEventBatteryInfo batteryInfo = new GBDeviceEventBatteryInfo();
        batteryInfo.state = BatteryState.UNKNOWN;
        if (json.has("chg")) {
            batteryInfo.state = (json.getInt("chg") == 1) ? BatteryState.BATTERY_CHARGING : BatteryState.BATTERY_NORMAL;
        }
        if (json.has("bat")) {
            int b = json.getInt("bat");
            if (b < 0) b = 0;
            if (b > 100) b = 100;
            // smooth out battery level reporting (it can only go up if charging, or down if discharging)
            // http://forum.espruino.com/conversations/379294
            if (lastBatteryPercent<0) lastBatteryPercent = b;
            if (batteryInfo.state == BatteryState.BATTERY_NORMAL && b > lastBatteryPercent)
                b = lastBatteryPercent;
            if (batteryInfo.state == BatteryState.BATTERY_CHARGING && b < lastBatteryPercent)
                b = lastBatteryPercent;
            lastBatteryPercent = b;
            batteryInfo.level = b;
        }

        if (json.has("volt"))
            batteryInfo.voltage = (float) json.getDouble("volt");
        handleGBDeviceEvent(batteryInfo);
    }

    /**
     * Handle "notify" packet, used to send notification control from device to GB
     */
    private void handleNotificationControl(JSONObject json) throws JSONException {

        String response = json.getString("n").toUpperCase(Locale.US);
        LOG.debug("Notification response: " + response);

        // Wake the Android device if the setting is toggled on by user.
        // Doesn't work if run after the notification handling below for some reason (which
        // I'd rather do since the screen would only be opened once the content was ready).
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (devicePrefs.getBoolean(PREF_NOTIFICATION_WAKE_ON_OPEN, false) && response.equals("OPEN")) {
            WakeActivity.start(getContext());
        }

        GBDeviceEventNotificationControl deviceEvtNotificationControl = new GBDeviceEventNotificationControl();
        // .title appears unused
        deviceEvtNotificationControl.event = GBDeviceEventNotificationControl.Event.valueOf(response);
        if (json.has("id"))
            deviceEvtNotificationControl.handle = json.getInt("id");
        if (json.has("tel"))
            deviceEvtNotificationControl.phoneNumber = json.getString("tel");
        if (json.has("msg"))
            deviceEvtNotificationControl.reply = json.getString("msg");
        /* REPLY responses don't use the ID from the event (MUTE/etc seem to), but instead
         * they use a handle that was provided in an action list on the onNotification.. event  */
        if (deviceEvtNotificationControl.event == GBDeviceEventNotificationControl.Event.REPLY) {
            Long foundHandle = mNotificationReplyAction.lookup((int)deviceEvtNotificationControl.handle);
            if (foundHandle!=null)
                deviceEvtNotificationControl.handle = foundHandle;
        }
        evaluateGBDeviceEvent(deviceEvtNotificationControl);
    }

    private void handleActivityFetch(final JSONObject json) throws JSONException {
        final String state = json.getString("state");
        if ("start".equals(state)) {
            GB.updateTransferNotification(getContext().getString(R.string.busy_task_fetch_activity_data),"", true, 0, getContext());
            getDevice().setBusyTask(getContext().getString(R.string.busy_task_fetch_activity_data));
        } else if ("end".equals(state)) {
            saveLastSyncTimestamp(System.currentTimeMillis() - 1000L * 60);
            getDevice().unsetBusyTask();
            GB.updateTransferNotification(null, "", false, 100, getContext());
        } else {
            LOG.warn("Unknown actfetch state {}", state);
        }

        final GBDeviceEventUpdatePreferences event = new GBDeviceEventUpdatePreferences()
                .withPreference(PREF_BANGLEJS_ACTIVITY_FULL_SYNC_STATUS, state);
        evaluateGBDeviceEvent(event);

        getDevice().sendDeviceUpdateIntent(getContext());
    }

    /**
     * Handle "act" packet, used to send activity reports
     */
    private void handleActivity(JSONObject json) {
        BangleJSActivitySample sample = new BangleJSActivitySample();
        int timestamp = (int) (json.optLong("ts", System.currentTimeMillis()) / 1000);
        int hrm = json.optInt("hrm", 0);
        int steps = json.optInt("stp", 0);
        int intensity = json.optInt("mov", ActivitySample.NOT_MEASURED);
        boolean realtime = json.optInt("rt", 0) == 1;
        ActivityKind activity = ActivityKind.ACTIVITY;
        if (json.has("act")) {
            try {
                String actName = json.optString("act","").toUpperCase(Locale.US);
                activity = ActivityKind.valueOf(actName);
            } catch (final Exception e) {
                LOG.warn("JSON activity not known", e);
                activity = ActivityKind.UNKNOWN;
            }
        }
        if(hrm>0) {
            sleepAsAndroidSender.onHrChanged(hrm, 0);
        }
        sample.setTimestamp(timestamp);
        sample.setRawKind(activity.getCode());
        sample.setHeartRate(hrm);
        sample.setSteps(steps);
        sample.setRawIntensity(intensity);
        if (!realtime) {
            try (DBHandler dbHandler = GBApplication.acquireDB()) {
                final Long userId = getUser(dbHandler.getDaoSession()).getId();
                final Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
                BangleJSSampleProvider provider = new BangleJSSampleProvider(getDevice(), dbHandler.getDaoSession());
                sample.setDeviceId(deviceId);
                sample.setUserId(userId);
                provider.upsertSample(sample);
            } catch (final Exception ex) {
                LOG.warn("Error saving activity: " + ex.getLocalizedMessage());
            }
        }

        // push realtime data
        if (realtime && (realtimeHRM || realtimeStep)) {
            Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                    .putExtra(GBDevice.EXTRA_DEVICE, getDevice())
                    .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
            LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
        }
    }

    /**
     * Handle "http" packet: make an HTTP request and return a "http" response
     */
    private void handleHttp(JSONObject json) throws JSONException {
        String _id = null;
        try {
            _id = json.getString("id");
        } catch (JSONException e) {
        }
        final String id = _id;

        if (! BuildConfig.INTERNET_ACCESS) {
            uartTxJSONError("http", "Internet access not enabled, check Gadgetbridge Device Settings", id);
            return;
        }

        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (! devicePrefs.getBoolean(PREF_DEVICE_INTERNET_ACCESS, false)) {
            uartTxJSONError("http", "Internet access not enabled in this Gadgetbridge build", id);
            return;
        }

        String url = json.getString("url");

        int method = Request.Method.GET;
        if (json.has("method")) {
            String m = json.getString("method").toLowerCase(Locale.US);
            if (m.equals("get")) method = Request.Method.GET;
            else if (m.equals("post")) method = Request.Method.POST;
            else if (m.equals("head")) method = Request.Method.HEAD;
            else if (m.equals("put")) method = Request.Method.PUT;
            else if (m.equals("patch")) method = Request.Method.PATCH;
            else if (m.equals("delete")) method = Request.Method.DELETE;
            else uartTxJSONError("http", "Unknown HTTP method "+m,id);
        }

        byte[] _body = null;
        if (json.has("body"))
            _body = json.getString("body").getBytes();
        final byte[] body = _body;

        Map<String,String> _headers = null;
        if (json.has("headers")) {
            JSONObject h = json.getJSONObject("headers");
            _headers = new HashMap<String,String>();
            Iterator<String> iter = h.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                try {
                    String value = h.getString(key);
                    _headers.put(key, value);
                } catch (JSONException e) {
                }
            }
        }
        final Map<String,String> headers = _headers;

        String _xmlPath = "";
        String _xmlReturn = "";
        try {
            _xmlPath = json.getString("xpath");
            _xmlReturn = json.getString("return");
        } catch (JSONException e) {
        }
        final String xmlPath = _xmlPath;
        final String xmlReturn = _xmlReturn;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(method, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject o = new JSONObject();
                        if (xmlPath.length() != 0) {
                            try {
                                InputSource inputXML = new InputSource(new StringReader(response));
                                XPath xPath = XPathFactory.newInstance().newXPath();
                                if (xmlReturn.equals("array")) {
                                    NodeList result = (NodeList) xPath.evaluate(xmlPath, inputXML, XPathConstants.NODESET);
                                    response = null; // don't add it below
                                    JSONArray arr = new JSONArray();
                                    if (result != null) {
                                        for (int i = 0; i < result.getLength(); i++)
                                            arr.put(result.item(i).getTextContent());
                                    }
                                    o.put("resp", arr);
                                } else {
                                    response = xPath.evaluate(xmlPath, inputXML);
                                }
                            } catch (Exception error) {
                                uartTxJSONError("http", error.toString(), id);
                                return;
                            }
                        }
                        try {
                            o.put("t", "http");
                            if( id!=null)
                                o.put("id", id);
                            if (response!=null)
                                o.put("resp", response);
                        } catch (JSONException e) {
                            GB.toast(getContext(), "HTTP: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
                        }
                        uartTxJSON("http", o);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                uartTxJSONError("http", error.toString(), id);
            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                if (body == null) return super.getBody();
                return body;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                // clone the data from super.getHeaders() so we can write to it
                Map<String, String> h = new HashMap<>(super.getHeaders());
                if (headers != null) {
                    for (String key : headers.keySet()) {
                        String value = headers.get(key);
                        h.put(key, value);
                    }
                }
                return h;
            }
        };
        RequestQueue queue = getRequestQueue();
        queue.add(stringRequest);
    }

    /**
     * Handle "force_calendar_sync" packet
     */
    private void handleCalendarSync(JSONObject json) throws JSONException {
        if (!getDevicePrefs().getBoolean("sync_calendar", false)) {
            LOG.debug("Ignoring calendar sync request, sync is disabled");
            return;
        }
        //pretty much like the updateEvents in CalendarReceiver, but would need a lot of libraries here
        JSONArray ids = json.getJSONArray("ids");
        ArrayList<Long> idsList = new ArrayList<>(ids.length());
        ArrayList<Long> idsDeletedList = new ArrayList<>(ids.length());
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
                    idsDeletedList.add(id);
                    onDeleteCalendarEvent((byte)0, id);
                    LOG.info("event id="+ id +" is on device id="+ deviceId +", removing it there");
                } else {
                    //used for later, no need to check twice the ones that do not match
                    idsList.add(id);
                }
            }
            // Now issue the command to delete from the Bangle
            if (idsDeletedList.size() > 0)
                deleteCalendarEvents(idsDeletedList);

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
        CalendarReceiver.forceSync();
    }

    /**
     * Handle "intent" packet: broadcast an Android intent
     */
    private void handleIntent(JSONObject json) throws JSONException {
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (!devicePrefs.getBoolean(PREF_DEVICE_INTENTS, false)) {
            uartTxJSONError("intent", "Android Intents not enabled, check Gadgetbridge Device Settings", null);
            return;
        }

        String target = json.has("target") ? json.getString("target") : "broadcastreceiver";
        Intent in = new Intent();
        if (json.has("action")) in.setAction(json.getString("action"));
        if (json.has("flags")) {
            JSONArray flags = json.getJSONArray("flags");
            for (int i = 0; i < flags.length(); i++) {
                in = addIntentFlag(in, flags.getString(i));
            }
        }
        if (json.has("categories")) {
            JSONArray categories = json.getJSONArray("categories");
            for (int i = 0; i < categories.length(); i++) {
                in.addCategory(categories.getString(i));
            }
        }
        if (json.has("package") && !json.has("class")) {
            in = json.getString("package").equals("gadgetbridge") ?
                    in.setPackage(this.getContext().getPackageName()) :
                    in.setPackage(json.getString("package"));
        }
        if (json.has("package") && json.has("class")) {
            in = json.getString("package").equals("gadgetbridge") ?
                    in.setClassName(this.getContext().getPackageName(), json.getString("class")) :
                    in.setClassName(json.getString("package"), json.getString("class"));
        }

        if (json.has("mimetype")) in.setType(json.getString("mimetype"));
        if (json.has("data")) in.setData(Uri.parse(json.getString("data")));
        if (json.has("extra")) {
            JSONObject extra = json.getJSONObject("extra");
            Iterator<String> iter = extra.keys();
            while (iter.hasNext()) {
                String key = iter.next();
                in.putExtra(key, extra.getString(key)); // Should this be implemented for other types, e.g. extra.getInt(key)? Or will this always work even if receiving ints/doubles/etc.?
            }
        }
        LOG.info("Executing intent:\n\t" + String.valueOf(in) + "\n\tTargeting: " + target);
        //GB.toast(getContext(), String.valueOf(in), Toast.LENGTH_LONG, GB.INFO);
        switch (target) {
            case "broadcastreceiver":
                getContext().sendBroadcast(in);
                break;
            case "activity": // See wakeActivity.java if you want to start activities from under the keyguard/lock sceen.
                getContext().startActivity(in);
                break;
            case "service": // Should this be implemented differently, e.g. workManager?
                getContext().startService(in);
                break;
            case "foregroundservice": // Should this be implemented differently, e.g. workManager?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getContext().startForegroundService(in);
                } else {
                    getContext().startService(in);
                }
                break;
            default:
                LOG.info("Targeting '"+target+"' isn't implemented or doesn't exist.");
                GB.toast(getContext(), "Targeting '"+target+"' isn't implemented or it doesn't exist.", Toast.LENGTH_LONG, GB.INFO);
        }
    }

    private Intent addIntentFlag(Intent intent, String flag) {
        try {
            final Class<Intent> intentClass = Intent.class;
            final Field flagField = intentClass.getDeclaredField(flag);
            intent.addFlags(flagField.getInt(null));
        } catch (final Exception e) {
            // The user sent an invalid flag
            LOG.info("Flag '"+flag+"' isn't implemented or doesn't exist and was therefore not set.");
            GB.toast(getContext(), "Flag '"+flag+"' isn't implemented or it doesn't exist and was therefore not set.", Toast.LENGTH_LONG, GB.INFO);
        }
        return intent;
    }

    private void handleFile(JSONObject json) throws JSONException {

        File dir;
        try {
            dir = new File(FileUtils.getExternalFilesDir() + "/" + FileUtils.makeValidFileName(getDevice().getName()));
            if (!dir.isDirectory()) {
                if (!dir.mkdir()) {
                    throw new IOException("Cannot create device specific directory for " + getDevice().getName());
                }
            }
        } catch (IOException e) {
            LOG.error("Could not get directory to write to with error: " + e);
            return;
        }
        String filename = json.getString("n");
        String filenameThatCantEscapeDir = filename.replaceAll("/","");

        LOG.debug("Compare filename and filenameThatCantEscapeDir:\n" + filename + "\n" + filenameThatCantEscapeDir);
        File outputFile = new File(dir, filenameThatCantEscapeDir);
        String mode = "append";
        if (json.getString("m").equals("w")) {
            mode = "write";
        }
        try {
            FileUtils.copyStringToFile(json.getString("c"), outputFile, mode);
            LOG.info("Writing to "+outputFile);
        } catch (IOException e) {
            LOG.warn("Could not write to " + outputFile + "with error: " + e);
        }
    }

    @Override
    public void onSendConfiguration(final String config) {
        switch (config) {
            case PREF_BANGLEJS_ACTIVITY_FULL_SYNC_START:
                fetchActivityData(0);
                return;
        }

        LOG.warn("Unknown config changed: {}", config);
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
            // Scan for flow control characters
            for (int i=0;i<chars.length;i++) {
                boolean ignoreChar = false;
                if (chars[i]==19 /* XOFF */) {
                    getQueue().setPaused(true);
                    LOG.info("RX: XOFF");
                    ignoreChar = true;
                }
                if (chars[i]==17 /* XON */) {
                    getQueue().setPaused(false);
                    LOG.info("RX: XON");
                    ignoreChar = true;
                }
                if (ignoreChar) {
                    // remove char from the array. Generally only one XON/XOFF per stream so creating a new array each time is fine
                    byte[] c = new byte[chars.length - 1];
                    System.arraycopy(chars, 0, c, 0, i); // copy before
                    System.arraycopy(chars, i+1, c, i, chars.length - i - 1); // copy after
                    chars = c;
                    i--; // back up one (because we deleted it)
                }
            }
            String packetStr = new String(chars, StandardCharsets.ISO_8859_1);
            LOG.debug("RX: " + packetStr);
            // logging
            addReceiveHistory(packetStr);
            // split into input lines
            receivedLine += packetStr;
            while (receivedLine.contains("\n")) {
                int p = receivedLine.indexOf("\n");
                String line = receivedLine.substring(0,(p>0) ? (p-1) : 0);
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
      cmd += "(s=>s&&(s.timezone="+tz+",require('Storage').write('setting.json',s)))(require('Storage').readJSON('setting.json',1))";
      uartTx(builder, cmd+"\n");
    }

    void requestBangleGPSPowerStatus() {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "is_gps_active");
            LOG.debug("Requesting gps power status: " + o.toString());
            uartTxJSON("is_gps_active", o);
        } catch (JSONException e) {
            GB.toast(getContext(), "uartTxJSONError: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    void setupGPSUpdateTimer() {
        if (gpsUpdateSetup) {
            LOG.debug("GPS position timer is already setup");
            return;
        }
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        if(devicePrefs.getBoolean(PREF_DEVICE_GPS_UPDATE, false)) {
            int intervalLength = devicePrefs.getInt(PREF_DEVICE_GPS_UPDATE_INTERVAL, 1000);
            LOG.info("Setup location listener with an update interval of " + intervalLength + " ms");
            boolean onlyUseNetworkGPS = devicePrefs.getBoolean(PREF_DEVICE_GPS_USE_NETWORK_ONLY, false);
            LOG.info("Using combined GPS and NETWORK based location: " + onlyUseNetworkGPS);
            if (!onlyUseNetworkGPS) {
                try {
                    GBLocationService.start(getContext(), getDevice(), GBLocationProviderType.GPS, intervalLength);
                } catch (IllegalArgumentException e) {
                    LOG.warn("GPS provider could not be started", e);
                }
            }

            try {
                GBLocationService.start(getContext(), getDevice(), GBLocationProviderType.NETWORK, intervalLength);
            } catch (IllegalArgumentException e) {
                LOG.warn("NETWORK provider could not be started", e);
            }
        } else {
            GB.toast("Phone gps data update is deactivated in the settings", Toast.LENGTH_SHORT, GB.INFO);
        }
        gpsUpdateSetup = true;
    }

    @Override
    public void onSetGpsLocation(final Location location) {
        if (!GBApplication.getPrefs().getBoolean("use_updated_location_if_available", false)) return;
        LOG.debug("new location: " + location.toString());
        JSONObject o = new JSONObject();
        try {
            o.put("t", "gps");
            o.put("lat", location.getLatitude());
            o.put("lon", location.getLongitude());
            o.put("alt", location.getAltitude());
            o.put("speed", location.getSpeed()*3.6); // m/s to kph
            if (location.hasBearing()) o.put("course", location.getBearing());
            o.put("time", location.getTime());
            if (location.getExtras() != null) {
                LOG.debug("Found number of satellites: " + location.getExtras().getInt("satellites", -1));
                o.put("satellites",location.getExtras().getInt("satellites"));
            } else {
                o.put("satellites", 0);
            }
            o.put("hdop", location.getAccuracy());
            o.put("externalSource", true);
            o.put("gpsSource", location.getProvider());
            LOG.debug("Sending gps value: " + o.toString());
            uartTxJSON("gps", o);
        } catch (JSONException e) {
            GB.toast(getContext(), "uartTxJSONError: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }



    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private String renderUnicodeWordPartAsImage(final String word) {
        // check for emoji
        final EmojiManager emojiManager = EmojiConverter.getEmojiManager(getContext());
        final boolean hasEmoji = !EmojiParserKt.extractEmojis(emojiManager, word).isEmpty();
        // if we had emoji, ensure we create 3 bit color (not 1 bit B&W)
        final BangleJSBitmapStyle style = hasEmoji ? BangleJSBitmapStyle.RGB_3BPP_TRANSPARENT : BangleJSBitmapStyle.MONOCHROME_TRANSPARENT;
        return "\0"+bitmapToEspruinoString(textToBitmap(word), style);
    }

    private String renderUnicodeWordAsImage(String word) {
        // if we have Chinese/Japanese/Korean chars, split into 2 char chunks to allow easier text wrapping
        // it's not perfect but better than nothing
        boolean hasCJK = false;
        for (int i=0;i<word.length();i++) {
            char ch = word.charAt(i);
            hasCJK |= ch>=0x4E00 && ch<=0x9FFF; // "CJK Unified Ideographs" block
        }
        if (hasCJK) {
            // split every 2 chars
            StringBuilder result = new StringBuilder();
            for (int i=0;i<word.length();i+=2) {
                int len = 2;
                if (i+len > word.length())
                    len = word.length()-i;
                result.append(renderUnicodeWordPartAsImage(word.substring(i, i + len)));
            }
            return result.toString();
        }
        // else just render the word as-is
        return renderUnicodeWordPartAsImage(word);
    }

    /* is the given character code an emoji? Note that `char` is only 0..65535, so many emoji will
      actually be made of 2 surrogate chars. To work around this, we just assume that anything with a surrogate
      is an emoji, which will be true in most cases */
    public boolean isCharCodeEmoji(char ch) {
        if (ch>=0x2190 && ch<=0x21FF) return true;
        if (ch>=0x2600 && ch<=0x26FF) return true;
        if (ch>=0x2700 && ch<=0x27BF) return true;
        if (ch>=0x3000 && ch<=0x303F) return true;
        if (ch>=0xD800 && ch<=0xDFFF) return true; // high/low surrogate
        if (ch>=0xFE00 && ch<=0xFE0F) return true; // variation selector 1 (2 is U+E0100 so is in a surrogate)
        //if (ch>=0x1F300 && ch<=0x1F64F) return true; // needs a surrogate
        //if (ch>=0x1F680 && ch<=0x1F6FF) return true; // needs a surrogate
        return false;
    }

    public String renderUnicodeAsImage(String txt) {
        // FIXME: it looks like we could implement this as customStringFilter now so it happens automatically
        if (txt==null) return null;
        // Simple conversions
        txt = txt.replaceAll("…", "...");
        /* If we're not doing conversion, pass this right back (we use the EmojiConverter
        As we would have done if BangleJSCoordinator.supportsUnicodeEmojis had reported false */
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        if (!devicePrefs.getBoolean(PREF_BANGLEJS_TEXT_BITMAP, false))
            return EmojiConverter.convertUnicodeEmojiToAscii(txt, GBApplication.getContext());
         // Otherwise split up and check each word
        String word = "";
        StringBuilder result = new StringBuilder();
        boolean needsTranslate = false;
        boolean wordIsAllEmoji = true;
        for (int i=0;i<txt.length();i++) {
            char ch = txt.charAt(i);
            // Special cases where we can just use a built-in character...
            // Based on https://op.europa.eu/en/web/eu-vocabularies/formex/physical-specifications/character-encoding
            if (ch=='–' || ch=='‐' || ch=='—') ch='-';
            else if (ch =='‚' || ch=='，' || ch=='、') ch=',';
            else if (ch =='。') ch='.';
            else if (ch =='【') ch='[';
            else if (ch =='】') ch=']';
            else if (ch=='‘' || ch=='’' || ch=='‛' || ch=='′' || ch=='ʹ') ch='\'';
            else if (ch=='“' || ch=='”' || ch =='„' || ch=='‟' || ch=='″') ch='"';
            else if (ch == 0xFEFF) continue; // nonbreaking space - ignore
            boolean isCharEmoji = isCharCodeEmoji(ch);
            if (isCharEmoji) {
                if (!wordIsAllEmoji) {
                    // if the word is all emoji we are fine, just fall through.
                    // but here we have characters already and we want to translate those
                    // separately, so we can have a color emoji on its own.
                    if (needsTranslate) { // convert word
                        LOG.info("renderUnicodeAsImage converting " + word);
                        result.append(renderUnicodeWordAsImage(word));
                    } else { // or just copy across
                        result.append(word);
                    }
                    word = "";
                    wordIsAllEmoji = true;
                }
                needsTranslate = true;
            } else {
                if (!word.isEmpty() && wordIsAllEmoji) {
                    // this isn't am emoji, but it follows one - render that separately!
                    result.append(renderUnicodeWordAsImage(word));
                    word = "";
                    needsTranslate = false;
                }
                wordIsAllEmoji = ch>=0xFE00;
            }
            // chars which break words up
            if (" -_/:.,?!'\"&*()[]".indexOf(ch)>=0) {
                // word split
                if (needsTranslate) { // convert word
                    LOG.info("renderUnicodeAsImage converting " + word);
                    result.append(renderUnicodeWordAsImage(word)).append(ch);
                } else { // or just copy across
                    result.append(word).append(ch);
                }
                word = "";
                needsTranslate = false;
                wordIsAllEmoji = true;
            } else {
                // TODO: better check?
                if (ch>255) needsTranslate = true;
                word += ch;
            }
        }
        if (needsTranslate) { // convert word
            LOG.info("renderUnicodeAsImage converting " + word);
            result.append(renderUnicodeWordAsImage(word));
        } else { // or just copy across
            result.append(word);
        }
        return result.toString();
    }

    /// Crop a text string to ensure it's not longer than requested
    public String cropToLength(String txt, int len) {
        if (txt==null) return "";
        if (txt.length()<=len) return txt;
        return txt.substring(0,len-3)+"...";
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        if (!getDevicePrefs().getBoolean(DeviceSettingsPreferenceConst.PREF_SEND_APP_NOTIFICATIONS, true)) {
            LOG.debug("App notifications disabled - ignoring");
            return;
        }

        boolean canReply = false;
        if (notificationSpec.attachedActions!=null)
            for (int i=0;i<notificationSpec.attachedActions.size();i++) {
                NotificationSpec.Action action = notificationSpec.attachedActions.get(i);
                if (action.type==NotificationSpec.Action.TYPE_WEARABLE_REPLY) {
                    mNotificationReplyAction.add(notificationSpec.getId(), action.handle);
                    canReply = true;
                }
            }
        // sourceName isn't set for SMS messages
        String src = notificationSpec.sourceName;
        if (notificationSpec.type == NotificationType.GENERIC_SMS)
            src = "SMS Message";
        // Send JSON to Bangle.js
        try {
            JSONObject o = new JSONObject();
            o.put("t", "notify");
            o.put("id", notificationSpec.getId());
            o.put("src", src);
            o.put("title", renderUnicodeAsImage(cropToLength(notificationSpec.title,80)));
            o.put("subject", renderUnicodeAsImage(cropToLength(notificationSpec.subject,80)));
            o.put("body", renderUnicodeAsImage(cropToLength(notificationSpec.body, 400)));
            o.put("sender", renderUnicodeAsImage(cropToLength(notificationSpec.sender,40)));
            o.put("tel", notificationSpec.phoneNumber);
            if (canReply) o.put("reply", true);
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
            // = we should generalize the pebble calender code
            forceCalendarSync();
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "canned_responses_sync");
            JSONArray jsonMessages = new JSONArray();
            o.put("d", jsonMessages);

            for (String message : cannedMessagesSpec.cannedMessages) {
                JSONObject jsonMessage = new JSONObject();
                jsonMessages.put(jsonMessage);
                // Render unicode (emojis etc.) as an image for BangleJS to display
                String unicodeRenderedAsImage = renderUnicodeAsImage(message);
                // If the initial and rendered messages are not the same, include the rendered message as "disp(lay)" text so unicode is rendered on device
                if (!unicodeRenderedAsImage.equals(message)) {
                    jsonMessage.put("disp", unicodeRenderedAsImage);
                }
                jsonMessage.put("text", message);
            }
            uartTxJSON("onSetCannedMessages", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
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
                if (alarm.getUnused()) continue;
                JSONObject jsonalarm = new JSONObject();
                jsonalarms.put(jsonalarm);
                //Calendar calendar = AlarmUtils.toCalendar(alarm);
                jsonalarm.put("h", alarm.getHour());
                jsonalarm.put("m", alarm.getMinute());
                jsonalarm.put("rep", alarm.getRepetition());
                jsonalarm.put("on", alarm.getEnabled());
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
                Field[] fields = callSpec.getClass().getDeclaredFields();
                for (Field field : fields)
                    if (field.getName().startsWith("CALL_") && field.getInt(callSpec) == callSpec.command)
                        cmdName = field.getName().substring(5).toLowerCase(Locale.US);
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
    public void onFetchRecordedData(int dataTypes) {
        // FIXME: The fetches are currently mutually exclusive, otherwise the operations will
        // interrupt one-another
        if ((dataTypes & RecordedDataTypes.TYPE_ACTIVITY) != 0)  {
            fetchActivityData(getLastSuccessfulSyncTime());
        } else if ((dataTypes & RecordedDataTypes.TYPE_GPS_TRACKS) !=0) {
            JSONObject requestTracksListObj = BangleJSActivityTrack.compileTracksListRequest(getDevice(), getContext());
            uartTxJSON("requestActivityTracksList", requestTracksListObj);
        } else if ((dataTypes & RecordedDataTypes.TYPE_DEBUGLOGS) !=0) {
            File dir;
            try {
                dir = FileUtils.getExternalFilesDir();
            } catch (IOException e) {
                return;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
            String filename = "banglejs_debug_" + dateFormat.format(new Date()) + ".log";
            File outputFile = new File(dir, filename);
            LOG.warn("Writing log to "+outputFile.toString());
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
                writer.write(receiveHistory);
                writer.close();
                receiveHistory = "";
                GB.toast(getContext(), "Log written to "+filename, Toast.LENGTH_LONG, GB.INFO);
            } catch (IOException e) {
                LOG.warn("Could not write to file", e);
            }
        }
    }

    protected void fetchActivityData(final long timestampMillis) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "actfetch");
            o.put("ts", timestampMillis);
            uartTxJSON("fetch activity data", o);
        } catch (final JSONException e) {
            LOG.warn("Failed to fetch activity data", e);
        }
    }

    protected String getLastSyncTimeKey() {
        return "lastSyncTimeMillis";
    }

    protected void saveLastSyncTimestamp(final long timestamp) {
        final SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).edit();
        editor.putLong(getLastSyncTimeKey(), timestamp);
        editor.apply();
    }

    protected long getLastSuccessfulSyncTime() {
        long timeStampMillis = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()).getLong(getLastSyncTimeKey(), 0);
        if (timeStampMillis != 0) {
            return timeStampMillis;
        }
        final GregorianCalendar calendar = BLETypeConversions.createCalendar();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        return calendar.getTimeInMillis();
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
        try {
            final TransactionBuilder builder = performInitialized("screenshot");
            uartTx(builder, "\u0010g.dump()\n");
            builder.queue(getQueue());
        } catch (final IOException e) {
            GB.toast(getContext(), "Failed to get screenshot: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetHeartRateMeasurementInterval(int seconds) {
        realtimeHRMInterval = seconds;
        transmitActivityStatus();
    }

    private List<LoyaltyCard> filterSupportedCards(final List<LoyaltyCard> cards) {
        final List<LoyaltyCard> ret = new ArrayList<>();
        for (final LoyaltyCard card : cards) {
            // we hardcode here what is supported
            if (card.getBarcodeFormat() == BarcodeFormat.CODE_39 ||
                    card.getBarcodeFormat() == BarcodeFormat.CODABAR ||
                    card.getBarcodeFormat() == BarcodeFormat.EAN_8 ||
                    card.getBarcodeFormat() == BarcodeFormat.EAN_13 ||
                    card.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                    card.getBarcodeFormat() == BarcodeFormat.UPC_E ||
                    card.getBarcodeFormat() == BarcodeFormat.QR_CODE) {
                ret.add(card);
            }
        }
        return ret;
    }

    @Override
    public void onSetLoyaltyCards(final ArrayList<LoyaltyCard> cards) {
        final List<LoyaltyCard> supportedCards = filterSupportedCards(cards);
        try {
            JSONObject encoded_cards = new JSONObject();
            JSONArray a = new JSONArray();
            for (final LoyaltyCard card : supportedCards) {
                JSONObject o = new JSONObject();
                o.put("id", card.getId());
                o.put("name", renderUnicodeAsImage(cropToLength(card.getName(),40)));
                if (card.getBarcodeId() != null) {
                    o.put("value", card.getBarcodeId());
                } else {
                    o.put("value", card.getCardId());
                }
                if (card.getBarcodeFormat() != null)
                    o.put("type", card.getBarcodeFormat().toString());
                if (card.getExpiry() != null)
                    o.put("expiration", card.getExpiry().getTime()/1000);
                o.put("color", card.getColor());
                // we somehow cannot distinguish no balance defined with 0 P
                if (card.getBalance() != null && card.getBalance().signum() != 0
                        || card.getBalanceType() != null) {
                    // if currency is points it is not reported
                    String balanceType = card.getBalanceType() != null ?
                        card.getBalanceType().toString() : "P";
                    o.put("balance", renderUnicodeAsImage(cropToLength(card.getBalance() +
                                    " " + balanceType, 20)));
                }
                if (card.getNote() != null)
                    o.put("note", renderUnicodeAsImage(cropToLength(card.getNote(),200)));
                a.put(o);
            }
            encoded_cards.put("t", "cards");
            encoded_cards.put("d", a);
            uartTxJSON("onSetLoyaltyCards", encoded_cards);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {
        if (!getDevicePrefs().getBoolean("sync_calendar", false)) {
            LOG.debug("Ignoring add calendar event {}, sync is disabled", calendarEventSpec.id);
            return;
        }
        String description = calendarEventSpec.description;
        if (description != null) {
            // remove any HTML formatting
            if (description.startsWith("<html"))
                description = androidx.core.text.HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
            // Replace "-::~:~::~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~:~::~:~::-" lines from Google meet
            description = ("\n"+description+"\n").replaceAll("\n-[:~-]*\n","");
            // Replace ____________________ from MicrosoftTeams
            description = description.replaceAll("__________+", "");
            // replace double newlines and trim beginning and end
            description = description.replaceAll("\n\\s*\n","\n").trim();
        }
        try {
            JSONObject o = new JSONObject();
            o.put("t", "calendar");
            o.put("id", calendarEventSpec.id);
            o.put("type", calendarEventSpec.type); //implement this too? (sunrise and set)
            o.put("timestamp", calendarEventSpec.timestamp);
            o.put("durationInSeconds", calendarEventSpec.durationInSeconds);
            o.put("title", renderUnicodeAsImage(cropToLength(calendarEventSpec.title,40)));
            o.put("description", renderUnicodeAsImage(cropToLength(description,200)));
            o.put("location", renderUnicodeAsImage(cropToLength(calendarEventSpec.location,40)));
            o.put("calName", cropToLength(calendarEventSpec.calName,20));
            o.put("color", calendarEventSpec.color);
            o.put("allDay", calendarEventSpec.allDay);
            uartTxJSON("onAddCalendarEvent", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {
        // FIXME: CalenderReceiver will call this directly - can we somehow batch up delete calls and use deleteCalendarEvents?
        if (!getDevicePrefs().getBoolean("sync_calendar", false)) {
            LOG.debug("Ignoring delete calendar event {}, sync is disabled", id);
            return;
        }
        try {
            JSONObject o = new JSONObject();
            o.put("t", "calendar-");
            o.put("id", id);
            uartTxJSON("onDeleteCalendarEvent", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    /* Called when we need to get rid of multiple calendar events */
    public void deleteCalendarEvents(ArrayList<Long> ids) {
        if (!getDevicePrefs().getBoolean("sync_calendar", false)) {
            LOG.debug("Ignoring delete calendar events {}, sync is disabled", ids);
            return;
        }
        if (!ids.isEmpty())
            try {
                JSONObject o = new JSONObject();
                o.put("t", "calendar-");
                if (ids.size() == 1) {
                    o.put("id", ids.get(0));
                } else {
                    JSONArray a = new JSONArray();
                    for (long id : ids) a.put(id);
                    o.put("id", a);
                }
                uartTxJSON("onDeleteCalendarEvent", o);
            } catch (JSONException e) {
                LOG.info("JSONException: " + e.getLocalizedMessage());
            }
    }

    @Override
    public void onSendWeather(ArrayList<WeatherSpec> weatherSpecs) {
        WeatherSpec weatherSpec = weatherSpecs.get(0);
        try {
            JSONObject o = new JSONObject();
            o.put("t", "weather");
            o.put("temp", weatherSpec.currentTemp);
            o.put("hi", weatherSpec.todayMaxTemp);
            o.put("lo", weatherSpec.todayMinTemp );
            o.put("hum", weatherSpec.currentHumidity);
            o.put("rain", weatherSpec.precipProbability);
            o.put("uv", Math.round(weatherSpec.uvIndex*10)/10);
            o.put("code", weatherSpec.currentConditionCode);
            o.put("txt", weatherSpec.currentCondition);
            o.put("wind", Math.round(weatherSpec.windSpeed*100)/100.0);
            o.put("wdir", weatherSpec.windDirection);
            o.put("loc", weatherSpec.location);
            uartTxJSON("onSendWeather", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }

    public Bitmap textToBitmap(String text) {
        Paint paint = new Paint(0); // Paint.ANTI_ALIAS_FLAG not wanted as 1bpp
        Prefs devicePrefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()));
        paint.setTextSize(devicePrefs.getInt(PREF_BANGLEJS_TEXT_BITMAP_SIZE, 18));
        paint.setColor(0xFFFFFFFF);
        paint.setTextAlign(Paint.Align.LEFT);
        float baseline = -paint.ascent(); // ascent() is negative
        int width = (int) (paint.measureText(text) + 0.5f); // round
        int height = (int) (baseline + paint.descent() + 0.5f);
        if (width<1) width=1;
        if (height<1) height=1;
        Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        canvas.drawText(text, 0, baseline, paint);
        return image;
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }

    public enum BangleJSBitmapStyle {
        MONOCHROME, // 1bpp
        MONOCHROME_TRANSPARENT, // 1bpp, black = transparent
        RGB_3BPP, // 3bpp
        RGB_3BPP_TRANSPARENT // 3bpp, least used color as transparent
    }

    /** Used for writing single bits to an array */
    public static class BitWriter {
        int n;
        final byte[] bits;
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
        if (width>255) {
            LOG.warn("bitmapToEspruinoArray width of "+width+" > 255 (Espruino max) - cropping");
            width = 255;
        }
        if (height>255) {
            LOG.warn("bitmapToEspruinoArray height of "+height+" > 255 (Espruino max) - cropping");
            height = 255;
        }
        int bpp = (style==BangleJSBitmapStyle.RGB_3BPP ||
                   style==BangleJSBitmapStyle.RGB_3BPP_TRANSPARENT) ? 3 : 1;
        byte[] pixels = new byte[width * height];
        final byte PIXELCOL_TRANSPARENT = -1;
        final int[] ditherMatrix = {1*16,5*16,7*16,3*16}; // for bayer dithering
        // if doing RGB_3BPP_TRANSPARENT, check image to see if it's transparent
        // MONOCHROME_TRANSPARENT is handled later on...
        boolean allowTransparency = (style == BangleJSBitmapStyle.RGB_3BPP_TRANSPARENT);
        boolean isTransparent = false;
        byte transparentColorIndex = 0;
        /* Work out what colour index each pixel should be and write to pixels.
         Also figure out if we're transparent at all, and how often each color is used */
        int[] colUsage = new int[8];
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
                if (bpp==1)
                    col = ((r+g+b) >= 768)?1:0;
                else if (bpp==3)
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
        // if we're MONOCHROME_TRANSPARENT, force transparency on bg color
        if (style == BangleJSBitmapStyle.MONOCHROME_TRANSPARENT) {
            isTransparent = true;
            transparentColorIndex = 0;
        }
        // Write the header
        int headerLen = isTransparent ? 4 : 3;
        byte[] bmp = new byte[(((height * width * bpp) + 7) >> 3) + headerLen];
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
        bits.finish();
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

    @Override
    public void onSetNavigationInfo(NavigationInfoSpec navigationInfoSpec) {
        try {
            JSONObject o = new JSONObject();
            o.put("t", "nav");
            if (navigationInfoSpec.instruction!=null)
                o.put("instr", navigationInfoSpec.instruction);
            o.put("distance", navigationInfoSpec.distanceToTurn);
            String[] navActions = {
                    "","continue", "left", "left_slight", "left_sharp",  "right", "right_slight",
                    "right_sharp", "keep_left", "keep_right", "uturn_left", "uturn_right",
                    "offroute", "roundabout_right", "roundabout_left", "roundabout_straight", "roundabout_uturn", "finish"};
            if (navigationInfoSpec.nextAction>0 && navigationInfoSpec.nextAction<navActions.length)
                o.put("action", navActions[navigationInfoSpec.nextAction]);
            if (navigationInfoSpec.ETA!=null)
                o.put("eta", navigationInfoSpec.ETA);
            uartTxJSON("onSetNavigationInfo", o);
        } catch (JSONException e) {
            LOG.info("JSONException: " + e.getLocalizedMessage());
        }
    }
}
