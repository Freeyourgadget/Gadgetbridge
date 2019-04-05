/*  Copyright (C) 2017-2019 Andreas Shimokawa, Carsten Pfeiffer

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

/*
    Features:
                                            Working
                                                    work in progress
                                                            possible
                                                                    not supported
    Get firmware version                    (X)     ()      (X)     ()
    Get battery level                       (X)     ()      (X)     ()

    Set alarms (1-3)                        (X)     ()      (X)     ()
    Sync date and time                      (X)     ()      (X)     ()
    Find device                             (X)     ()      (X)     ()

    Switch 12/24 hour mode                  ()      ()      (X)     ()
    Set step goal                           ()      ()      (X)     ()
    Set sitting reminder                    ()      ()      (X)     ()
    Trigger a photo                         ()      ()      (X)     ()

    Switch automated heartbeat detection    (X)     ()      (X)     ()
    Switch display illumination             ()      ()      (X)     ()
    Switch vibration                        ()      ()      (X)     ()
    Switch notifications                    ()      ()      (X)     ()
    Set do not distract time                ()      ()      (X)     ()

    Get Steps                               ()      ()      (X)     ()
    Get Heart Rate                          ()      ()      (X)     ()
    Get Blood Pressure                      ()      ()      (x)     ()
    Get Blood Satiation                     ()      ()      (X)     ()

    Send Notification                       ()      ()      (X)     ()

 */


package nodomain.freeyourgadget.gadgetbridge.service.devices.bfh16;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.pebble.GBDeviceEventDataLogging;
import nodomain.freeyourgadget.gadgetbridge.devices.bfh16.BFH16Constants;
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
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class BFH16DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(BFH16DeviceSupport.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public BFH16DeviceSupport() {
        super(LOG);
        addSupportedService(BFH16Constants.BFH16_SERVICE1);
        addSupportedService(BFH16Constants.BFH16_SERVICE2);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing BFH16");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(BFH16Constants.BFH16_SERVICE1_NOTIFY);
        ctrlCharacteristic = getCharacteristic(BFH16Constants.BFH16_SERVICE1_WRITE);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        syncSettings(builder);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        LOG.info("Initialization BFH16 Done");

        return builder;
    }

    //onXYZ
    //______________________________________________________________________________________________

    //TODO check TODOs in method
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
            case BFH16Constants.RECEIVE_DEVICE_INFO:
                int fwVerNum = data[4] & 0xFF;
                versionCmd.fwVersion = (fwVerNum / 100) + "." + ((fwVerNum % 100) / 10) + "." + ((fwVerNum % 100) % 10);
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case BFH16Constants.RECEIVE_BATTERY_LEVEL:
                batteryCmd.level = data[8];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + batteryCmd.level);
                return true;
            case BFH16Constants.RECEIVE_STEPS_DATA:
                int steps = ByteBuffer.wrap(data, 5, 4).getInt();
                //TODO handle step data
                LOG.info("Number of walked steps: " + steps);
                return true;
            case BFH16Constants.RECEIVE_HEART_DATA:
                //TODO handle heart data
                LOG.info("Current heart rate: "     + data[1]);
                LOG.info("Current blood pressure: " + data[2] + "/" + data[3]);
                LOG.info("Current satiation: "      + data[4]);
                return true;
            case BFH16Constants.RECEIVE_PHOTO_TRIGGER:
                //TODO handle photo trigger
                LOG.info("Received photo trigger: " + data[8]);
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                LOG.info("Unhandled characteristic data: "+ data[0]+" "+data[1]+" "+data[2]+" "+data[3]+" "+data[4]+" "+data[5]+" "+data[6]+" "+data[7]+" "+data[8]);
                return true;
        }
    }


    //working
    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("SetAlarms");

            for (int i = 0; i < alarms.size(); i++)
            {
                byte cmd;
                switch (i) {
                    case 0:
                        cmd = BFH16Constants.CMD_SET_ALARM_1;
                        break;
                    case 1:
                        cmd = BFH16Constants.CMD_SET_ALARM_2;
                        break;
                    case 2:
                        cmd = BFH16Constants.CMD_SET_ALARM_3;
                        break;
                    default:
                        return;
                }
                Calendar cal = AlarmUtils.toCalendar(alarms.get(i));
                builder.write(ctrlCharacteristic, commandWithChecksum(
                        cmd,
                        alarms.get(i).getEnabled() ? cal.get(Calendar.HOUR_OF_DAY) : -1,
                        alarms.get(i).getEnabled() ? cal.get(Calendar.MINUTE) : -1
                ));
            }
            builder.queue(getQueue());
            GB.toast(getContext(), "Alarm settings applied - do note that the current device does not support day specification", Toast.LENGTH_LONG, GB.INFO);
        } catch(IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    //working
    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("SetTime");
            syncDateAndTime(builder);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.warn(e.getMessage());
        }
    }

    //working
    @Override
    public void onFindDevice(boolean start) {
        try {
            TransactionBuilder builder = performInitialized("FindDevice");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    BFH16Constants.CMD_VIBRATE, 0, start ? 1 : 0
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
        GB.toast(getContext(), "Your device will vibrate 3 times!", Toast.LENGTH_LONG, GB.INFO);
    }


    //TODO: checked + rework
    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String notificationTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        byte icon;
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                icon = BFH16Constants.ICON_SMS;
                break;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                icon = BFH16Constants.ICON_FACEBOOK;
                break;
            case TWITTER:
                icon = BFH16Constants.ICON_TWITTER;
                break;
            case WHATSAPP:
                icon = BFH16Constants.ICON_WHATSAPP;
                break;
            default:
                icon = BFH16Constants.ICON_LINE;
                break;
        }
        showNotification(icon, notificationTitle, notificationSpec.body);
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    //TODO: check
    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(BFH16Constants.ICON_CALL, callSpec.name, callSpec.number);
                break;
        }
    }

    //TODO: check
    @Override
    public void onEnableRealtimeSteps(boolean enable) {
        onEnableRealtimeHeartRateMeasurement(enable);
    }

    //TODO: check
    @Override
    public void onReset(int flags) {
        try {
            TransactionBuilder builder = performInitialized("Reboot");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    BFH16Constants.CMD_ACTION_REBOOT_DEVICE, 0, 0
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    //TODO: check
    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    BFH16Constants.CMD_MEASURE_HEART, 0, 1
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    //TODO: check
    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        // TODO: test
        try {
            TransactionBuilder builder = performInitialized("RealTimeHeartMeasurement");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    BFH16Constants.CMD_MEASURE_HEART, 0, enable ? 1 : 0
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    //TODO: check
    @Override
    public void onSetConstantVibration(int integer) {
        try {
            TransactionBuilder builder = performInitialized("Vibrate");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    BFH16Constants.CMD_VIBRATE, 0, 1
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {

    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {

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
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

    @Override
    public void onTestNewFunction() {

        showNotification((byte)0xFF, "", "");

//        try {
//            TransactionBuilder builder = performInitialized("TestNewFunction");
//
//            //Test get sleep time
//            builder.write(ctrlCharacteristic, commandWithChecksum( (byte)0x32, 0, 0));
//            builder.queue(getQueue());
//
//            GB.toast(getContext(), "TestNewFunction executed!", Toast.LENGTH_LONG, GB.INFO);
//
//        } catch(IOException e) {
//            LOG.warn(e.getMessage());
//        }

    }





    //FUNCTIONS
    //______________________________________________________________________________________________

    //TODO: check
    private void showNotification(byte icon, String title, String message) {
        try {
            TransactionBuilder builder = performInitialized("ShowNotification");

            byte[] titleBytes = stringToUTF8Bytes(title, 16);
            byte[] messageBytes = stringToUTF8Bytes(message, 80);

            for (int i = 1; i <= 7; i++)
            {
                byte[] currentPacket = new byte[20];
                currentPacket[0] = BFH16Constants.CMD_ACTION_SHOW_NOTIFICATION;
                currentPacket[1] = 7;
                currentPacket[2] = (byte)i;
                switch(i) {
                    case 1:
                        currentPacket[4] = icon;
                        break;
                    case 2:
                        if (titleBytes != null) {
                            System.arraycopy(titleBytes, 0, currentPacket, 3, 6);
                            System.arraycopy(titleBytes, 6, currentPacket, 10, 10);
                        }
                        break;
                    default:
                        if (messageBytes != null) {
                            System.arraycopy(messageBytes, 16 * (i - 3), currentPacket, 3, 6);
                            System.arraycopy(messageBytes, 6 + 16 * (i - 3), currentPacket, 10, 10);
                        }
                        break;
                }
                builder.write(ctrlCharacteristic, currentPacket);
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn(e.getMessage());
        }
    }


    //TODO: check
    private void syncSettings(TransactionBuilder builder) {
        syncDateAndTime(builder);

        // TODO: unhardcode and separate stuff
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_HEARTRATE_WARNING_VALUE, 0, 152
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_TARGET_STEPS, 0, 10000
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SWITCH_METRIC_IMPERIAL, 0, 0
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_GET_SLEEP_TIME, 0, 0
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_NOON_TIME, 12 * 60 * 60, 14 * 60 * 60 // 12:00 - 14:00
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_SLEEP_TIME, 21 * 60 * 60, 8 * 60 * 60 // 21:00 - 08:00
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_INACTIVITY_WARNING_TIME, 0, 0
        ));

        // do not disturb and a couple more features
        byte dndStartHour = 22;
        byte dndStartMin = 0;
        byte dndEndHour = 8;
        byte dndEndMin = 0;
        boolean dndToggle = false;
        boolean vibrationToggle = true;
        boolean wakeOnRaiseToggle = true;
        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_DND_SETTINGS,
                (dndStartHour << 24) | (dndStartMin << 16) | (dndEndHour << 8) | dndEndMin,
                ((dndToggle ? 0 : 1) << 2) | ((vibrationToggle ? 1 : 0) << 1) | (wakeOnRaiseToggle ? 1 : 0)
        ));
    }


    //working
    private void syncDateAndTime(TransactionBuilder builder) {
        Calendar cal = Calendar.getInstance();
        String strYear = String.valueOf(cal.get(Calendar.YEAR));
        byte year1 = (byte)Integer.parseInt(strYear.substring(0, 2));
        byte year2 = (byte)Integer.parseInt(strYear.substring(2, 4));
        byte month = (byte)cal.get(Calendar.MONTH);
        byte day = (byte)cal.get(Calendar.DAY_OF_MONTH);
        byte hour = (byte)cal.get(Calendar.HOUR_OF_DAY);
        byte minute = (byte)cal.get(Calendar.MINUTE);
        byte second = (byte)cal.get(Calendar.SECOND);
        byte weekDay = (byte)cal.get(Calendar.DAY_OF_WEEK);

        builder.write(ctrlCharacteristic, commandWithChecksum(
                BFH16Constants.CMD_SET_DATE_AND_TIME,
                (year1 << 24) | (year2 << 16) | (month << 8) | day,
                (hour << 24) | (minute << 16) | (second << 8) | weekDay
        ));
    }

    //working
    private byte[] commandWithChecksum(byte cmd, int argSlot1, int argSlot2)
    {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put(cmd);
        buf.putInt(argSlot1);
        buf.putInt(argSlot2);

        byte[] bytesToWrite = buf.array();

        byte checksum = 0;
        for (byte b : bytesToWrite) {
            checksum += b;
        }

        bytesToWrite[9] = checksum;

        return bytesToWrite;
    }

    /**
     * Checksum is calculated by the sum of bytes 0 to 8 and send as byte 9
     */
    private byte[] commandWithChecksum(byte s0, byte s1, byte s2, byte s3, byte s4, byte s5, byte s6, byte s7, byte s8)
    {
        ByteBuffer buf = ByteBuffer.allocate(10);
        buf.put(s0);
        buf.put(s1);
        buf.put(s2);
        buf.put(s3);
        buf.put(s4);
        buf.put(s5);
        buf.put(s6);
        buf.put(s7);
        buf.put(s8);

        byte[] bytesToWrite = buf.array();

        byte checksum = 0;
        for (byte b : bytesToWrite) {
            checksum += b;
        }

        //checksum = (byte) ((byte) checksum & (byte) 0xFF);  //TODO EXPERIMENTAL

        LOG.debug("Checksum = " + checksum);

        bytesToWrite[9] = checksum;

        return bytesToWrite;
    }

    private byte[] stringToUTF8Bytes(String src, int byteCount) {
        try {
            if (src == null)
                return null;

            for (int i = src.length(); i > 0; i--) {
                String sub = src.substring(0, i);
                byte[] subUTF8 = sub.getBytes("UTF-8");

                if (subUTF8.length == byteCount) {
                    return subUTF8;
                }

                if (subUTF8.length < byteCount) {
                    byte[] largerSubUTF8 = new byte[byteCount];
                    System.arraycopy(subUTF8, 0, largerSubUTF8, 0, subUTF8.length);
                    return largerSubUTF8;
                }
            }
        } catch (UnsupportedEncodingException e) {
            LOG.warn(e.getMessage());
        }
        return null;
    }

}
