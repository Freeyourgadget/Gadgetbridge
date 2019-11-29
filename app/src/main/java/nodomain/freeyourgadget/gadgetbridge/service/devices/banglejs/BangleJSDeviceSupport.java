package nodomain.freeyourgadget.gadgetbridge.service.devices.banglejs;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.widget.Toast;
import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;
import org.json.JSONArray;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.banglejs.BangleJSConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.no1f1.No1F1Constants;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class BangleJSDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(BangleJSDeviceSupport.class);
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public BluetoothGattCharacteristic rxCharacteristic = null;
    public BluetoothGattCharacteristic txCharacteristic = null;

    private String receivedLine = "";

    public BangleJSDeviceSupport() {
        super(LOG);
        addSupportedService(BangleJSConstants.UUID_SERVICE_NORDIC_UART);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        rxCharacteristic = getCharacteristic(BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX);
        txCharacteristic = getCharacteristic(BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_TX);
        builder.setGattCallback(this);
        builder.notify(rxCharacteristic, true);

        setTime(builder);
        //sendSettings(builder);

        // get version

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        LOG.info("Initialization Done");

        return builder;
    }

    /// Write a string of data, and chunk it up
    public void uartTx(TransactionBuilder builder, String str) {
        byte bytes[];
        try {
            bytes = str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("TX: UnsupportedEncodingException");
            return;
        }
        for (int i=0;i<bytes.length;i+=20) {
            int l = bytes.length-i;
            if (l>20) l=20;
            byte packet[] = new byte[l];
            for (int b=0;b<l;b++)
                packet[b] = bytes[i+b];
            builder.write(txCharacteristic, packet);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }
        if (BangleJSConstants.UUID_CHARACTERISTIC_NORDIC_UART_RX.equals(characteristic.getUuid())) {
            byte chars[] = characteristic.getValue();
            String packetStr = new String(chars);
            LOG.info("RX: " + packetStr);
            receivedLine += packetStr;
            while (receivedLine.indexOf("\n")>=0) {
                int p = receivedLine.indexOf("\n");
                String line =  receivedLine.substring(0,p-1);
                receivedLine = receivedLine.substring(p+1);
                LOG.info("RX LINE: " + line);
                // TODO: parse this into JSON and handle it
            }
        }
        return false;
    }


    void setTime(TransactionBuilder builder) {

        uartTx(builder, "setTime("+(System.currentTimeMillis()/1000)+");E.setTimeZone("+(TimeZone.getDefault().getRawOffset()/3600000)+");\n");
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        try {
            TransactionBuilder builder = performInitialized("onNotification");
            JSONObject o = new JSONObject();
            o.put("t", "notify");
            o.put("id", notificationSpec.getId());
            o.put("src", notificationSpec.sourceName);
            o.put("title", notificationSpec.title);
            o.put("subject", notificationSpec.subject);
            o.put("body", notificationSpec.body);
            o.put("sender", notificationSpec.sender);
            o.put("tel", notificationSpec.phoneNumber);

            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting notification: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onDeleteNotification(int id) {
        try {
            TransactionBuilder builder = performInitialized("onDeleteNotification");
            JSONObject o = new JSONObject();
            o.put("t", "notify-");
            o.put("id", id);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error deleting notification: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("setTime");
            setTime(builder);
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting time: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("onSetAlarms");
            JSONObject o = new JSONObject();
            o.put("t", "alarm");
            JSONArray jsonalarms = new JSONArray();
            o.put("d", jsonalarms);

            for (Alarm alarm : alarms) {
                if (!alarm.getEnabled()) continue;
                JSONObject jsonalarm = new JSONObject();
                jsonalarms.put(jsonalarm);

                Calendar calendar = AlarmUtils.toCalendar(alarm);
                // TODO: getRepetition to ensure it only happens on correct day?
                jsonalarm.put("h", alarm.getHour());
                jsonalarm.put("m", alarm.getMinute());
            }

            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting alarms: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        try {
            TransactionBuilder builder = performInitialized("onSetCallState");
            JSONObject o = new JSONObject();
            o.put("t", "call");
            String cmdString[] = {"","undefined","accept","incoming","outgoing","reject","start","end"};
            o.put("cmd", cmdString[callSpec.command]);
            o.put("name", callSpec.name);
            o.put("number", callSpec.number);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting call state: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        try {
            TransactionBuilder builder = performInitialized("onSetMusicState");
            JSONObject o = new JSONObject();
            o.put("t", "musicstate");
            String musicStates[] = {"play","pause","stop",""};
            o.put("state", musicStates[stateSpec.state]);
            o.put("position", stateSpec.position);
            o.put("shuffle", stateSpec.shuffle);
            o.put("repeat", stateSpec.repeat);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting Music state: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        try {
            TransactionBuilder builder = performInitialized("onSetMusicInfo");
            JSONObject o = new JSONObject();
            o.put("t", "musicinfo");
            o.put("artist", musicSpec.artist);
            o.put("album", musicSpec.album);
            o.put("track", musicSpec.track);
            o.put("dur", musicSpec.duration);
            o.put("c", musicSpec.trackCount);
            o.put("n", musicSpec.trackNr);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error setting Music info: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {

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

    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("onFindDevice");
            JSONObject o = new JSONObject();
            o.put("t", "find");
            o.put("n", start);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error finding device: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {
        try {
            TransactionBuilder builder = performInitialized("onSetConstantVibration");
            JSONObject o = new JSONObject();
            o.put("t", "vibrate");
            o.put("n", integer);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error vibrating: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
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
            TransactionBuilder builder = performInitialized("onSendWeather");
            JSONObject o = new JSONObject();
            o.put("t", "weather");
            o.put("temp", weatherSpec.currentTemp);
            o.put("hum", weatherSpec.currentHumidity);
            o.put("txt", weatherSpec.currentCondition);
            o.put("wind", weatherSpec.windSpeed);
            o.put("loc", weatherSpec.location);
            uartTx(builder, "\u0010gb("+o.toString()+")\n");
            builder.queue(getQueue());
        } catch (Exception e) {
            GB.toast(getContext(), "Error showing weather: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }
}
