/*  Copyright (C) 2017-2018 Andreas Shimokawa, Sami Alaoui

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.jyou;

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
import nodomain.freeyourgadget.gadgetbridge.devices.jyou.JYouConstants;
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
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class TeclastH30Support extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(TeclastH30Support.class);

    public BluetoothGattCharacteristic ctrlCharacteristic = null;
    public BluetoothGattCharacteristic measureCharacteristic = null;

    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();

    public TeclastH30Support() {
        super(LOG);
        addSupportedService(JYouConstants.UUID_SERVICE_JYOU);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        measureCharacteristic = getCharacteristic(JYouConstants.UUID_CHARACTERISTIC_MEASURE);
        ctrlCharacteristic = getCharacteristic(JYouConstants.UUID_CHARACTERISTIC_CONTROL);

        builder.setGattCallback(this);
        builder.notify(measureCharacteristic, true);

        syncSettings(builder);

        gbDevice.setState(GBDevice.State.INITIALIZED);
        gbDevice.sendDeviceUpdateIntent(getContext());

        LOG.info("Initialization Done");

        return builder;
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
            case JYouConstants.RECEIVE_DEVICE_INFO:
                int fwVerNum = data[4] & 0xFF;
                versionCmd.fwVersion = (fwVerNum / 100) + "." + ((fwVerNum % 100) / 10) + "." + ((fwVerNum % 100) % 10);
                handleGBDeviceEvent(versionCmd);
                LOG.info("Firmware version is: " + versionCmd.fwVersion);
                return true;
            case JYouConstants.RECEIVE_BATTERY_LEVEL:
                batteryCmd.level = data[8];
                handleGBDeviceEvent(batteryCmd);
                LOG.info("Battery level is: " + batteryCmd.level);
                return true;
            case JYouConstants.RECEIVE_STEPS_DATA:
                int steps = ByteBuffer.wrap(data, 5, 4).getInt();
                LOG.info("Number of walked steps: " + steps);
                return true;
            case JYouConstants.RECEIVE_HEARTRATE:
                LOG.info("Current heart rate: " + data[8]);
                return true;
            default:
                LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
                return true;
        }
    }

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
                JYouConstants.CMD_SET_DATE_AND_TIME,
                (year1 << 24) | (year2 << 16) | (month << 8) | day,
                (hour << 24) | (minute << 16) | (second << 8) | weekDay
        ));
    }

    private void syncSettings(TransactionBuilder builder) {
        syncDateAndTime(builder);

        // TODO: unhardcode and separate stuff
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_HEARTRATE_WARNING_VALUE, 0, 152
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_TARGET_STEPS, 0, 10000
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_GET_STEP_COUNT, 0, 0
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_GET_SLEEP_TIME, 0, 0
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_NOON_TIME, 12 * 60 * 60, 14 * 60 * 60 // 12:00 - 14:00
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_SLEEP_TIME, 21 * 60 * 60, 8 * 60 * 60 // 21:00 - 08:00
        ));
        builder.write(ctrlCharacteristic, commandWithChecksum(
                JYouConstants.CMD_SET_INACTIVITY_WARNING_TIME, 0, 0
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
                JYouConstants.CMD_SET_DND_SETTINGS,
                (dndStartHour << 24) | (dndStartMin << 16) | (dndEndHour << 8) | dndEndMin,
                ((dndToggle ? 0 : 1) << 2) | ((vibrationToggle ? 1 : 0) << 1) | (wakeOnRaiseToggle ? 1 : 0)
        ));
    }

    private void showNotification(byte icon, String title, String message) {
        try {
            TransactionBuilder builder = performInitialized("ShowNotification");

            byte[] titleBytes = stringToUTF8Bytes(title, 16);
            byte[] messageBytes = stringToUTF8Bytes(message, 80);

            for (int i = 1; i <= 7; i++)
            {
                byte[] currentPacket = new byte[20];
                currentPacket[0] = JYouConstants.CMD_ACTION_SHOW_NOTIFICATION;
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

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String notificationTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        byte icon;
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                icon = JYouConstants.ICON_SMS;
                break;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                icon = JYouConstants.ICON_FACEBOOK;
                break;
            case TWITTER:
                icon = JYouConstants.ICON_TWITTER;
                break;
            case WHATSAPP:
                icon = JYouConstants.ICON_WHATSAPP;
                break;
            default:
                icon = JYouConstants.ICON_LINE;
                break;
        }
        showNotification(icon, notificationTitle, notificationSpec.body);
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("SetAlarms");

            for (int i = 0; i < alarms.size(); i++)
            {
                byte cmd;
                switch (i) {
                    case 0:
                        cmd = JYouConstants.CMD_SET_ALARM_1;
                        break;
                    case 1:
                        cmd = JYouConstants.CMD_SET_ALARM_2;
                        break;
                    case 2:
                        cmd = JYouConstants.CMD_SET_ALARM_3;
                        break;
                    default:
                        return;
                }
                Calendar cal = alarms.get(i).getAlarmCal();
                builder.write(ctrlCharacteristic, commandWithChecksum(
                        cmd,
                        alarms.get(i).isEnabled() ? cal.get(Calendar.HOUR_OF_DAY) : -1,
                        alarms.get(i).isEnabled() ? cal.get(Calendar.MINUTE) : -1
                ));
            }
            builder.queue(getQueue());
            GB.toast(getContext(), "Alarm settings applied - do note that the current device does not support day specification", Toast.LENGTH_LONG, GB.INFO);
        } catch(IOException e) {
            LOG.warn(e.getMessage());
        }
    }

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

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(JYouConstants.ICON_CALL, callSpec.name, callSpec.number);
                break;
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
        try {
            TransactionBuilder builder = performInitialized("Reboot");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_ACTION_REBOOT_DEVICE, 0, 0
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onHeartRateTest() {
        try {
            TransactionBuilder builder = performInitialized("HeartRateTest");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_ACTION_HEARTRATE_SWITCH, 0, 1
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {
        // TODO: test
        try {
            TransactionBuilder builder = performInitialized("RealTimeHeartMeasurement");
            builder.write(ctrlCharacteristic, commandWithChecksum(
                    JYouConstants.CMD_SET_HEARTRATE_AUTO, 0, enable ? 1 : 0
            ));
            builder.queue(getQueue());
        } catch(Exception e) {
            LOG.warn(e.getMessage());
        }
    }

    @Override
    public void onFindDevice(boolean start) {
        if (start) {
            showNotification(JYouConstants.ICON_QQ, "Gadgetbridge", "Bzzt! Bzzt!");
            GB.toast(getContext(), "As your device doesn't have sound, it will only vibrate 3 times consecutively", Toast.LENGTH_LONG, GB.INFO);
        }
    }

    @Override
    public void onSetConstantVibration(int integer) {

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
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }

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
