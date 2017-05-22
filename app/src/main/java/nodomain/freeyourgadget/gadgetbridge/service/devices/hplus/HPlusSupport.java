/*  Copyright (C) 2016-2017 Alberto, Andreas Shimokawa, Carsten Pfeiffer,
    ivanovlev, João Paulo Barraca

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.hplus.HPlusCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;
import nodomain.freeyourgadget.gadgetbridge.model.GenericItem;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;


public class HPlusSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(HPlusSupport.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;

    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    private HPlusHandlerThread syncHelper;
    private DeviceType deviceType = DeviceType.UNKNOWN;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String s = intent.getAction();
            if (s.equals(DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    };

    public HPlusSupport(DeviceType type) {
        super(LOG);
        LOG.info("HPlusSupport Instance Created");
        deviceType = type;

        addSupportedService(HPlusConstants.UUID_SERVICE_HP);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        IntentFilter intentFilter = new IntentFilter();

        broadcastManager.registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void dispose() {
        LOG.info("Dispose");
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());
        broadcastManager.unregisterReceiver(mReceiver);

        close();

        super.dispose();
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_CONTROL);


        builder.notify(getCharacteristic(HPlusConstants.UUID_CHARACTERISTIC_MEASURE), true);
        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);
        //Initialize device
        sendUserInfo(builder); //Sync preferences

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        if(syncHelper == null) {
            syncHelper = new HPlusHandlerThread(getDevice(), getContext(), this);
            syncHelper.start();
        }
        syncHelper.sync();

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        requestDeviceInfo(builder);

        LOG.info("Initialization Done");

        return builder;
    }

    private HPlusSupport sendUserInfo(TransactionBuilder builder) {
        builder.write(ctrlCharacteristic, HPlusConstants.CMD_SET_PREF_START);
        builder.write(ctrlCharacteristic, HPlusConstants.CMD_SET_PREF_START1);

        syncPreferences(builder);

        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_CONF_END});
        return this;
    }

    private HPlusSupport syncPreferences(TransactionBuilder transaction) {

        if (deviceType == DeviceType.HPLUS) {
            setSIT(transaction);          //Sync SIT Interval
        }

        setCurrentDate(transaction);
        setCurrentTime(transaction);
        setDayOfWeek(transaction);
        setTimeMode(transaction);

        setGender(transaction);
        setAge(transaction);
        setWeight(transaction);
        setHeight(transaction);

        setGoal(transaction);
        setLanguage(transaction);
        setScreenTime(transaction);
        setUnit(transaction);
        setAllDayHeart(transaction);

        return this;
    }

    private HPlusSupport setLanguage(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getLanguage(getDevice().getAddress());
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_LANGUAGE,
                value
        });
        return this;
    }


    private HPlusSupport setTimeMode(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getTimeMode(getDevice().getAddress());

        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_TIMEMODE,
                value
        });
        return this;
    }

    private HPlusSupport setUnit(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getUnit(getDevice().getAddress());
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_UNITS,
                value
        });
        return this;
    }

    private HPlusSupport setCurrentDate(TransactionBuilder transaction) {
        Calendar c = GregorianCalendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_DATE,
                (byte) ((year / 256) & 0xff),
                (byte) (year % 256),
                (byte) (month + 1),
                (byte) (day)

        });
        return this;
    }

    private HPlusSupport setCurrentTime(TransactionBuilder transaction) {
        Calendar c = GregorianCalendar.getInstance();

        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_TIME,
                (byte) c.get(Calendar.HOUR_OF_DAY),
                (byte) c.get(Calendar.MINUTE),
                (byte) c.get(Calendar.SECOND)

        });
        return this;
    }


    private HPlusSupport setDayOfWeek(TransactionBuilder transaction) {
        Calendar c = GregorianCalendar.getInstance();

        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_WEEK,
                (byte) (c.get(Calendar.DAY_OF_WEEK) - 1)
        });
        return this;
    }


    private HPlusSupport setSIT(TransactionBuilder transaction) {

        //Makibes F68 doesn't like this command.
        //Just ignore.
        if (deviceType == DeviceType.MAKIBESF68) {
            return this;
        }

        int startTime = HPlusCoordinator.getSITStartTime(getDevice().getAddress());
        int endTime = HPlusCoordinator.getSITEndTime(getDevice().getAddress());

        Calendar now = GregorianCalendar.getInstance();

        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_SIT_INTERVAL,
                (byte) ((startTime / 256) & 0xff),
                (byte) (startTime % 256),
                (byte) ((endTime / 256) & 0xff),
                (byte) (endTime % 256),
                0,
                0,
                (byte) ((now.get(Calendar.YEAR) / 256) & 0xff),
                (byte) (now.get(Calendar.YEAR) % 256),
                (byte) (now.get(Calendar.MONTH) + 1),
                (byte) (now.get(Calendar.DAY_OF_MONTH)),
                (byte) (now.get(Calendar.HOUR_OF_DAY)),
                (byte) (now.get(Calendar.MINUTE)),
                (byte) (now.get(Calendar.SECOND)),
                0,
                0,
                0,
                0

        });
        return this;
    }

    private HPlusSupport setWeight(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getUserWeight();
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_WEIGHT,
                value

        });
        return this;
    }

    private HPlusSupport setHeight(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getUserHeight();
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_HEIGHT,
                value

        });
        return this;
    }


    private HPlusSupport setAge(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getUserAge();
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_AGE,
                value

        });
        return this;
    }

    private HPlusSupport setGender(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getUserGender();
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_GENDER,
                value

        });
        return this;
    }


    private HPlusSupport setGoal(TransactionBuilder transaction) {
        int value = HPlusCoordinator.getGoal();
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_GOAL,
                (byte) ((value / 256) & 0xff),
                (byte) (value % 256)

        });
        return this;
    }


    private HPlusSupport setScreenTime(TransactionBuilder transaction) {
        byte value = HPlusCoordinator.getScreenTime(getDevice().getAddress());
        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_SCREENTIME,
                value

        });
        return this;
    }

    private HPlusSupport setAllDayHeart(TransactionBuilder transaction) {

        byte value = HPlusCoordinator.getAllDayHR(getDevice().getAddress());

        transaction.write(ctrlCharacteristic, new byte[]{
                HPlusConstants.CMD_SET_ALLDAY_HRM,
                value

        });

        return this;
    }


    private HPlusSupport setAlarm(TransactionBuilder transaction, Calendar t) {

        byte hour = HPlusConstants.ARG_ALARM_DISABLE;
        byte minute = HPlusConstants.ARG_ALARM_DISABLE;

        if (t != null) {
            hour = (byte) t.get(Calendar.HOUR_OF_DAY);
            minute = (byte) t.get(Calendar.MINUTE);
        }

        transaction.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_ALARM,
                hour,
                minute});

        return this;
    }

    private HPlusSupport setFindMe(TransactionBuilder transaction, boolean state) {
        //TODO: Find how this works

        byte[] msg = new byte[2];
        msg[0] = HPlusConstants.CMD_SET_FINDME;

        if (state)
            msg[1] = HPlusConstants.ARG_FINDME_ON;
        else
            msg[1] = HPlusConstants.ARG_FINDME_OFF;

        transaction.write(ctrlCharacteristic, msg);
        return this;
    }

    private HPlusSupport requestDeviceInfo(TransactionBuilder builder) {
        // HPlus devices seem to report some information in an alternative manner
        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_DEVICE_ID});
        builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_GET_VERSION});

        return this;
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private void handleDeviceInfo(DeviceInfo info) {
        LOG.warn("Device info: " + info);
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        //TODO: Show different notifications according to source as Band supports this
        //LOG.info("OnNotification: Title: "+notificationSpec.title+" Body: "+notificationSpec.body+" Source: "+notificationSpec.sourceName+" Sender: "+notificationSpec.sender+" Subject: "+notificationSpec.subject);
        showText(notificationSpec.title, notificationSpec.body);
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {

        try {
            TransactionBuilder builder = performInitialized("time");

            setCurrentDate(builder);
            setCurrentTime(builder);
            performConnected(builder.getTransaction());
        }catch(IOException e){

        }
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {


        try {
            TransactionBuilder builder = performInitialized("alarm");

            for (Alarm alarm : alarms) {

                if (!alarm.isEnabled())
                    continue;

                if (alarm.isSmartWakeup()) //Not available
                    continue;

                Calendar t = alarm.getAlarmCal();
                setAlarm(builder, t);
                builder.queue(getQueue());

                GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT, GB.INFO);

                return; //Only first alarm
            }

            setAlarm(builder, null);
            performConnected(builder.getTransaction());

            GB.toast(getContext(), getContext().getString(R.string.user_feedback_all_alarms_disabled), Toast.LENGTH_SHORT, GB.INFO);
        }catch(Exception e){}


    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING: {
                showIncomingCall(callSpec.name, callSpec.number);
                break;
            }
        }

    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {
        LOG.info("Canned Messages: " + cannedMessagesSpec);
    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

    }

    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        onEnableRealtimeHeartRateMeasurement(enable);
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
    public void onAppConfiguration(UUID appUuid, String config) {

    }

    @Override
    public void onAppReorder(UUID[] uuids) {

    }

    @Override
    public void onFetchActivityData() {

        if (syncHelper == null){
            syncHelper = new HPlusHandlerThread(gbDevice, getContext(), this);
            syncHelper.start();
        }

        syncHelper.sync();
    }

    @Override
    public void onReboot() {
        try {
            getQueue().clear();

            TransactionBuilder builder = performInitialized("Shutdown");
            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SHUTDOWN, HPlusConstants.ARG_SHUTDOWN_EN});
            performConnected(builder.getTransaction());
        }catch(Exception e){

        }
    }

    @Override
    public void onHeartRateTest() {
        getQueue().clear();
        try{
            TransactionBuilder builder = performInitialized("HeartRateTest");

            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_HEARTRATE_STATE, HPlusConstants.ARG_HEARTRATE_MEASURE_ON}); //Set Real Time... ?
            performConnected(builder.getTransaction());
        }catch(Exception e){

        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        try {
            TransactionBuilder builder = performInitialized("realTimeHeartMeasurement");
            byte state;

            if (enable)
                state = HPlusConstants.ARG_HEARTRATE_ALLDAY_ON;
            else
                state = HPlusConstants.ARG_HEARTRATE_ALLDAY_OFF;

            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_ALLDAY_HRM, state});
            performConnected(builder.getTransaction());
        }catch(Exception e){

        }
    }

    @Override
    public void onFindDevice(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("findMe");

            setFindMe(builder, start);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error toggling Find Me: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }

    }

    @Override
    public void onSetConstantVibration(int intensity) {
        try {
            TransactionBuilder builder = performInitialized("vibration");

            byte[] msg = new byte[15];
            msg[0] = HPlusConstants.CMD_SET_INCOMING_CALL_NUMBER;

            for (int i = 0; i < msg.length - 1; i++)
                msg[i + 1] = (byte) "GadgetBridge".charAt(i);

            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error setting Vibration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onScreenshotReq() {

    }

    @Override
    public void onEnableHeartRateSleepSupport(boolean enable) {
        onEnableRealtimeHeartRateMeasurement(enable);

    }

    @Override
    public void onAddCalendarEvent(CalendarEventSpec calendarEventSpec) {

    }

    @Override
    public void onDeleteCalendarEvent(byte type, long id) {

    }

    @Override
    public void onSendConfiguration(String config) {
        LOG.info("Send Configuration: " + config);

    }

    @Override
    public void onTestNewFunction() {
        LOG.info("Test New Function");
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    public void setUnicodeSupport(boolean support){
        HPlusCoordinator.setUnicodeSupport(gbDevice.getAddress(), support);
    }


    private void showIncomingCall(String name, String rawNumber) {
        try {

            TransactionBuilder builder = performInitialized("incomingCall");

            //Enable call notifications
            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_ACTION_INCOMING_CALL, 1});

            //Show Call Icon
            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_INCOMING_CALL, HPlusConstants.ARG_INCOMING_CALL});

            if(name != null) {
                byte[] msg = new byte[13];

                //Show call name
                for (int i = 0; i < msg.length; i++)
                    msg[i] = ' ';

                byte[] nameBytes = encodeStringToDevice(name);
                for (int i = 0; i < nameBytes.length && i < (msg.length - 1); i++)
                    msg[i + 1] = nameBytes[i];

                msg[0] = HPlusConstants.CMD_ACTION_DISPLAY_TEXT_NAME;
                builder.write(ctrlCharacteristic, msg);

                msg[0] = HPlusConstants.CMD_ACTION_DISPLAY_TEXT_NAME_CN;
                builder.write(ctrlCharacteristic, msg);
            }

            if(rawNumber != null) {
                StringBuilder number = new StringBuilder();

                //Clean up number as the device only accepts digits
                for (char c : rawNumber.toCharArray()) {
                    if (Character.isDigit(c)) {
                        number.append(c);
                    }
                }

                byte[] msg = new byte[13];

                //Show call number
                for (int i = 0; i < msg.length; i++)
                    msg[i] = ' ';

                for (int i = 0; i < number.length() && i < (msg.length - 1); i++)
                    msg[i + 1] = (byte) number.charAt(i);

                msg[0] = HPlusConstants.CMD_SET_INCOMING_CALL_NUMBER;

                builder.wait(200);
                builder.write(ctrlCharacteristic, msg);
            }

            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing incoming call: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);

        }
    }

    private void showText(String title, String body) {
        try {
            TransactionBuilder builder = performInitialized("notification");

            String message = "";

            if (title != null && title.length() > 0) {
                message = StringUtils.pad(StringUtils.truncate(title, 16), 16); //Limit title to top row
            }

            if (body != null) {
                message += body;
            }

            byte[] messageBytes = encodeStringToDevice(message);

            int length = messageBytes.length / 17;

            length = length > 5 ? 5 : length;

            builder.write(ctrlCharacteristic, new byte[]{HPlusConstants.CMD_SET_INCOMING_MESSAGE, HPlusConstants.ARG_INCOMING_MESSAGE});

            int remaining = Math.min(255, (messageBytes.length % 17 > 0) ? length + 1 : length);

            byte[] msg = new byte[20];
            msg[0] = HPlusConstants.CMD_ACTION_DISPLAY_TEXT;
            msg[1] = (byte) remaining;

            for (int i = 2; i < msg.length; i++)
                msg[i] = ' ';

            int message_index = 0;
            int i = 3;

            for (int j = 0; j < messageBytes.length; j++) {
                msg[i++] = messageBytes[j];

                if (i == msg.length) {
                    message_index++;
                    msg[2] = (byte) message_index;
                    builder.write(ctrlCharacteristic, msg);

                    msg = msg.clone();
                    for (i = 3; i < msg.length; i++)
                        msg[i] = ' ';

                    if (message_index < remaining)
                        i = 3;
                    else
                        break;
                }
            }

            msg[2] = (byte) remaining;

            builder.write(ctrlCharacteristic, msg);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            GB.toast(getContext(), "Error showing device Notification: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);

        }
    }

    private void close() {
        if (syncHelper != null) {
            syncHelper.quit();
            syncHelper.interrupt();
            syncHelper = null;
        }
    }

    /**
     * HPlus devices accept a subset of GB2312 with some modifications.
     * This function will apply a custom transliteration.
     * While related to the methods implemented in LanguageUtils. These are specific for HPLUS
     *
     * @param s The String to transliterate
     * @return An array of bytes ready to be sent to the device
     */
    private byte[] encodeStringToDevice(String s) {

        List<Byte> outBytes = new ArrayList<Byte>();
        Boolean unicode = HPlusCoordinator.getUnicodeSupport(this.gbDevice.getAddress());
        LOG.info("Encode String: Unicode=" + unicode);

        for (int i = 0; i < s.length(); i++) {
            Character c = s.charAt(i);
            byte[] cs;

            if (HPlusConstants.transliterateMap.containsKey(c)) {
                cs = HPlusConstants.transliterateMap.get(c);
            } else {
                try {
                    if(HPlusCoordinator.getUnicodeSupport(this.gbDevice.getAddress()))
                    if(unicode)
                        cs = c.toString().getBytes("Unicode");
                    else
                        cs = c.toString().getBytes("GB2312");
                } catch (UnsupportedEncodingException e) {
                    //Fallback. Result string may be strange, but better than nothing
                    cs = c.toString().getBytes();
                    LOG.error("Could not convert String to Bytes: " + e.getMessage());
                }
            }
            for (int j = 0; j < cs.length; j++)
                outBytes.add(cs[j]);
        }

        return ArrayUtils.toPrimitive(outBytes.toArray(new Byte[outBytes.size()]));
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        switch (data[0]) {
            case HPlusConstants.DATA_VERSION:
            case HPlusConstants.DATA_VERSION1:
                return syncHelper.processVersion(data);

            case HPlusConstants.DATA_STATS:
                boolean result = syncHelper.processRealtimeStats(data, HPlusCoordinator.getUserAge());
                if (result) {
                    processExtraInfo (data);
                }
                return result;

            case HPlusConstants.DATA_SLEEP:
                return syncHelper.processIncomingSleepData(data);

            case HPlusConstants.DATA_STEPS:
                return syncHelper.processDaySummary(data);

            case HPlusConstants.DATA_DAY_SUMMARY:
            case HPlusConstants.DATA_DAY_SUMMARY_ALT:
                return syncHelper.processIncomingDaySlotData(data, HPlusCoordinator.getUserAge());
            case HPlusConstants.DATA_UNKNOWN:
                return true;
            default:

                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + Arrays.toString(data));
                return true;
        }
    }

    private void  processExtraInfo (byte[] data) {
        try {
            HPlusDataRecordRealtime record = new HPlusDataRecordRealtime(data, HPlusCoordinator.getUserAge());

            handleBatteryInfo(record.battery);

            String DEVINFO_STEP = getContext().getString(R.string.chart_steps) + ": ";
            String DEVINFO_DISTANCE = getContext().getString(R.string.distance) + ": ";
            String DEVINFO_CALORY = getContext().getString(R.string.calories) + ": ";
            String DEVINFO_HEART = getContext().getString(R.string.charts_legend_heartrate);

            String info = "";
            if (record.steps > 0) {
                info += DEVINFO_STEP + String.valueOf(record.steps) + "   ";
            }
            if (record.distance > 0) {
                info += DEVINFO_DISTANCE + String.valueOf(record.distance) + "   ";
            }
            if (record.calories > 0) {
                info += DEVINFO_CALORY + String.valueOf(record.calories) + "   ";
            }
            if (record.heartRate > 0) {
                info += DEVINFO_HEART + String.valueOf(record.heartRate) + "   ";
            }

            if (!info.equals("")) {
                getDevice().addDeviceInfo(new GenericItem("", info));
            }
        } catch (IllegalArgumentException e) {
            LOG.info((e.getMessage()));
        }
    }

    private void handleBatteryInfo(byte data) {
            if (batteryCmd.level != (short) data) {
                batteryCmd.level = (short) data;
                handleGBDeviceEvent(batteryCmd);
            }
    }

}
