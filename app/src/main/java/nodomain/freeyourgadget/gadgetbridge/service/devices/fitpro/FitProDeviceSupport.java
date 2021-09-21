/*  Copyright (C) 2016-2020 Petr Vaněk

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

package nodomain.freeyourgadget.gadgetbridge.service.devices.fitpro;

import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_ALARM;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_DND;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_FIND_BAND;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GET_HW_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_BAND_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_BIND;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_GENERAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_HEARTRATE_SETTINGS;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_RECEIVE_BUTTON_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_RECEIVE_SPORTS_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_REQUEST_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_GROUP_RESET;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_HEART_RATE_MEASUREMENT;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_NOTIFICATIONS_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_NOTIFICATION_CALL;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_NOTIFICATION_MESSAGE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_REQUEST_STEPS_DATA0x10;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_REQUEST_STEPS_DATA0x7;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_REQUEST_STEPS_DATA0x8;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_REQUEST_STEPS_DATA1;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_RESET;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_RX_BAND_INFO;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_ARM;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_DATE_TIME;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_DEVICE_VIBRATIONS;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_DISPLAY_ON_LIFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_LANGUAGE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_LONG_SIT_REMINDER;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_SLEEP_TIMES;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_STEP_GOAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_SET_USER_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_UNBIND;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.CMD_WEATHER;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.GENDER_FEMALE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.GENDER_MALE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_FACEBOOK;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_INSTAGRAM;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_LINE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_QQ;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_SMS;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_TWITTER;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_WECHAT;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.NOTIFICATION_ICON_WHATSAPP;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_CAMERA1;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_CAMERA2;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_CAMERA3;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_FIND_PHONE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_HEART_RATE_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_MEDIA_BACK;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_MEDIA_FORW;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_MEDIA_PLAY_PAUSE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_SLEEP_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_SPORTS_DAY_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.RX_STEP_DATA;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.UNIT_IMPERIAL;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.UNIT_METRIC;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.UUID_CHARACTERISTIC_RX;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.UUID_CHARACTERISTIC_TX;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_ON;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_ARM_LEFT;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_ARM_RIGHT;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_DEVICE_VIBRATIONS_DISABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_DEVICE_VIBRATIONS_ENABLE;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_LONG_SIT_REMINDER_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_LONG_SIT_REMINDER_ON;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_NOTIFICATIONS_ENABLE_OFF;
import static nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants.VALUE_SET_NOTIFICATIONS_ENABLE_ON;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.fitpro.FitProSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.FitProActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.CalendarEventSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.CannedMessagesSpec;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.model.MusicSpec;
import nodomain.freeyourgadget.gadgetbridge.model.MusicStateSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.Weather;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceBusyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.AlarmUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class FitProDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(FitProDeviceSupport.class);
    public final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    public final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    public final DeviceInfoProfile<FitProDeviceSupport> deviceInfoProfile;
    public final BatteryInfoProfile<FitProDeviceSupport> batteryInfoProfile;

    public BluetoothGattCharacteristic readCharacteristic;
    public BluetoothGattCharacteristic writeCharacteristic;
    private static final boolean debugEnabled = false;

    public FitProDeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);

        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String action = intent.getAction();
                if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                    handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
                } else if (BatteryInfoProfile.ACTION_BATTERY_INFO.equals(action)) {
                    handleBatteryInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo) intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO));
                }
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);

        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(mListener);
        addSupportedProfile(batteryInfoProfile);
        addSupportedService(FitProConstants.UUID_CHARACTERISTIC_RX);
        addSupportedService(FitProConstants.UUID_CHARACTERISTIC_UART);
    }

    @Override
    public TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        readCharacteristic = getCharacteristic(UUID_CHARACTERISTIC_RX);
        writeCharacteristic = getCharacteristic(UUID_CHARACTERISTIC_TX);

        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_RX), true);
        builder.notify(getCharacteristic(GattService.UUID_SERVICE_BATTERY_SERVICE), true);
        builder.setGattCallback(this);

        deviceInfoProfile.requestDeviceInfo(builder);
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);
        deviceInfoProfile.enableNotify(builder, true);

        // this sequence seems to be important as without it:
        // - fetch steps doesn't work
        // - band seems to drain battery really fast
        // - the wait time is needed as the band must process each command
        // - (implementation based on individual requests did not work, the wait is still needed)

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, FitProConstants.CMD_INIT1, (byte) 0x2));
        setTime(builder);
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, FitProConstants.CMD_INIT1));
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, FitProConstants.CMD_INIT2));
        builder.wait(200);
        setLanguage(builder);
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, FitProConstants.CMD_INIT3, VALUE_ON));
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, VALUE_ON));
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, (byte) 0xf));
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_REQUEST_DATA, CMD_GET_HW_INFO));
        builder.wait(200);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_BAND_INFO, CMD_RX_BAND_INFO));
        builder.wait(200);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        return builder;
    }

    public void handleDeviceInfo(DeviceInfo info) {
        LOG.debug("fitpro device info: " + info);
        versionCmd.hwVersion = "FitPro";
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    public void handleDeviceInfo(byte[] value) {
        LOG.debug("FitPro device info2");
        //test this 0xCD 0x00 0x11 0x15 0x01 0x02 0x00 0x0C 0x2B 0x27 0x00 0x01 0x33 0xA5 0x02 0x79 0x0A 0x68 0x56 0x06
        debugPrintArray(value, "Device info:");
        if (value.length < 20) {
            return;
        }
        int start = 14;
        int data_len = (int) value[start];

        byte[] name = new byte[data_len];
        System.arraycopy(value, start + 1, name, 0, data_len);
        String sName = new String(name, StandardCharsets.UTF_8); //unused for now

        start = start + data_len + 1;
        data_len = (int) value[start];
        byte[] hwname = new byte[data_len];
        System.arraycopy(value, start + 1, hwname, 0, data_len);
        String sHWName = new String(hwname, StandardCharsets.UTF_8);
        LOG.debug("Device info: " + versionCmd);
        versionCmd.hwVersion = sHWName;
        handleGBDeviceEvent(versionCmd);
    }

    public byte[] craftData(byte command_group, byte command, byte[] data) {
        //0xCD 0x00 0x09 0x12 0x01 0x01 0x00 0x04 0xA5 0x83 0x73 0xDB
        byte[] result = new byte[FitProConstants.DATA_TEMPLATE.length + data.length];
        System.arraycopy(FitProConstants.DATA_TEMPLATE, 0, result, 0, FitProConstants.DATA_TEMPLATE.length);
        result[2] = (byte) (FitProConstants.DATA_TEMPLATE.length + data.length - 3);
        result[3] = command_group;
        result[5] = command;
        result[7] = (byte) data.length;
        System.arraycopy(data, 0, result, 8, data.length);
        //debug
        debugPrintArray(result, "crafted packet");
        return result;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        debugPrintArray(data, "FitPro received value");
        if (data[0] != FitProConstants.DATA_HEADER) {
            if (debugEnabled) {
                LOG.info("FitPro, packet not starting with 0xcd: " + data[0]);
                debugPrintArray(new byte[]{data[0]}, "first byte");
                LOG.info("Characteristic changed UUID: " + characteristicUUID);
                LOG.info("Characteristic changed service: " + characteristic.getService().getCharacteristics());
                debugPrintArray(data, "value bytes");
            }
            indicateFinishedFetchingOperation();
            return false;
        }

        if (data != null && data.length > 5) {
            byte command = data[3];
            byte param = data[5];

            switch (command) {
                case CMD_GROUP_RECEIVE_BUTTON_DATA:
                    switch (param) {
                        case RX_FIND_PHONE:
                            handleFindPhone();
                            break;
                        case RX_MEDIA_BACK:
                        case RX_MEDIA_FORW:
                        case RX_MEDIA_PLAY_PAUSE:
                            handleMediaButton(param);
                            break;
                        case RX_CAMERA1:
                        case RX_CAMERA2:
                        case RX_CAMERA3:
                            handleCamera(param);
                            break;
                        default:
                    }
                    break;
                case CMD_GROUP_RECEIVE_SPORTS_DATA:
                    switch (param) {
                        case RX_HEART_RATE_DATA:
                            handleHR(data);
                            break;
                        case RX_SPORTS_DAY_DATA:
                            indicateStartingFetchingOperation();
                            handleDayTotalsData(data);
                            indicateFinishedFetchingOperation();
                            break;
                        case RX_SLEEP_DATA:
                            indicateStartingFetchingOperation();
                            handleSleepData(data);
                            indicateFinishedFetchingOperation();
                            break;
                        case RX_STEP_DATA:
                            indicateStartingFetchingOperation();
                            handleStepData(data);
                            indicateFinishedFetchingOperation();
                            break;
                        case CMD_REQUEST_STEPS_DATA0x7:
                        case CMD_REQUEST_STEPS_DATA0x8:
                        case CMD_REQUEST_STEPS_DATA0x10:
                            //acking this makes the band to send data
                            sendAck(data[3], data[1], data[2], data[5]);
                            break;
                    }
                    break;
                case CMD_GROUP_BAND_INFO:
                    switch (param) {
                        case CMD_RX_BAND_INFO:
                            handleDeviceInfo(data);
                            break;
                    }
                    sendAck(data[3], data[1], data[2], data[5]);
                    break;
                case CMD_GROUP_REQUEST_DATA:
                    switch (param) {
                        case CMD_GET_HW_INFO:
                            handleHardwareDetails(data);
                            break;
                    }
                    sendAck(data[3], data[1], data[2], data[5]);
                    break;
            }

            LOG.info("Characteristic changed UUID: " + characteristicUUID);
            LOG.info("Characteristic changed service: " + characteristic.getService().getCharacteristics());
            debugPrintArray(data, "value bytes");
        }
        return false;
    }

    public void indicateFinishedFetchingOperation() {
        //LOG.debug("download finish announced");
        GB.updateTransferNotification(null, "", false, 100, getContext());
        GB.signalActivityDataFinish();
        unsetBusy();
    }

    public void indicateStartingFetchingOperation() {
        GB.updateTransferNotification(null, getContext().getString(R.string.busy_task_fetch_activity_data), true, 10, getContext());
    }

    protected void unsetBusy() {
        if (getDevice().isBusy()) {
            getDevice().unsetBusyTask();
            getDevice().sendDeviceUpdateIntent(getContext());
        }
    }

    public void handleHardwareDetails(byte[] value) {
        LOG.debug("FitPro hardware details");
        debugPrintArray(value, "Device info:");
        if (value.length < 20) {
            return;
        }
        int start = 8;
        int data_len = (int) value[start];

        byte[] led = new byte[data_len];
        System.arraycopy(value, start + 1, led, 0, data_len);
        String sLED = new String(led, StandardCharsets.UTF_8);

        start = start + data_len + 1;
        data_len = (int) value[start];
        byte[] gsensor = new byte[data_len];
        System.arraycopy(value, start + 1, gsensor, 0, data_len);
        String sGsensor = new String(gsensor, StandardCharsets.UTF_8);

        gbDevice.setFirmwareVersion2(sGsensor + " " + sLED);

        //the band does not like to answer when asked together for both hw info, so ask now,
        // after data is already received

        TransactionBuilder builder = new TransactionBuilder("notification");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_BAND_INFO, CMD_RX_BAND_INFO));
        builder.queue(getQueue());

    }

    public void handleHR(byte[] value) {
        LOG.debug("FitPro handle heart rate measurement");
        debugPrintArray(value, "value");
        if (value.length < 17) {
            LOG.debug("FitPro heartrate measurement payload too short");
            return;
        }

        int heartRate = (int) value[19];
        int pressureLow = (int) value[18];
        int pressureHigh = (int) value[17];
        int spo2 = (int) value[13];
        int seconds = ByteBuffer.wrap(value, 12, 4).getInt();
        sendAck(value[3], value[1], value[2], value[5]);

        if (!(heartRate > 0)) {
            return;
        }
        handleHR(seconds, heartRate, pressureLow, pressureHigh, spo2);
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        LOG.debug("FitPro send call notification");
        TransactionBuilder builder = new TransactionBuilder("CALL");

        if (callSpec.command == CallSpec.CALL_INCOMING) {

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(0x1);
                outputStream.write(0x0);
                outputStream.write(0x0);

                if (callSpec.name != null) {
                    outputStream.write(callSpec.name.getBytes(StandardCharsets.UTF_8));
                    outputStream.write(0x20);
                }
                if (callSpec.number != null) {
                    outputStream.write(callSpec.number.getBytes(StandardCharsets.UTF_8));
                    outputStream.write(0x20);
                }

            } catch (IOException e) {
                LOG.error("error sending call notification: " + e);
            }
            debugPrintArray(craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_CALL, outputStream.toByteArray()), "crafted call notify");
            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_CALL, outputStream.toByteArray()));
        } else {
            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_CALL, VALUE_OFF));
        }
        builder.queue(getQueue());
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
    public void onSendConfiguration(String config) {

        LOG.debug("FitPro on send config: " + config);
        try {
            TransactionBuilder builder = performInitialized("sendConfiguration");
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_LANGUAGE:
                    setLanguage(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_LONGSIT_PERIOD:
                case DeviceSettingsPreferenceConst.PREF_LONGSIT_SWITCH:
                case DeviceSettingsPreferenceConst.PREF_LONGSIT_START:
                case DeviceSettingsPreferenceConst.PREF_LONGSIT_END:
                    setLongSitReminder(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_ACTIVATE_DISPLAY_ON_LIFT:
                case DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_START:
                case DeviceSettingsPreferenceConst.PREF_DISPLAY_ON_LIFT_END:
                    setDisplayOnLift(builder);
                    break;
                case SettingsActivity.PREF_MEASUREMENT_SYSTEM:
                case ActivityUser.PREF_USER_WEIGHT_KG:
                case ActivityUser.PREF_USER_GENDER:
                case ActivityUser.PREF_USER_HEIGHT_CM:
                case ActivityUser.PREF_USER_YEAR_OF_BIRTH:
                    setUserData(builder);
                    break;
                case ActivityUser.PREF_USER_STEPS_GOAL:
                    setStepsGoal(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_START:
                case DeviceSettingsPreferenceConst.PREF_DO_NOT_DISTURB_NOAUTO_END:
                    setDoNotDisturb(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME:
                case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_START:
                case DeviceSettingsPreferenceConst.PREF_SLEEP_TIME_END:
                    setSleepTime(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_WEARLOCATION:
                    setWearLocation(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_VIBRATION_ENABLE:
                    setVibrations(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE:
                    setNotifications(builder);
                    break;
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SWITCH:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_SLEEP:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_INTERVAL:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_START:
                case DeviceSettingsPreferenceConst.PREF_AUTOHEARTRATE_END:
                    setAutoHeartRate(builder);
                    break;
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast(getContext(), "Error sending configuration: " + e.getLocalizedMessage(), Toast.LENGTH_LONG, GB.ERROR);
        }
    }

    @Override
    public void onReadConfiguration(String config) {

    }

    public void sendAck(byte command_group, byte length_high, byte length_low, byte command) {
        LOG.debug(" ACKing data: " + nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils.arrayToString(new byte[]{command_group}) + " " + nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils.arrayToString(new byte[]{command}));
        TransactionBuilder builder = new TransactionBuilder("notification");
        short size = (short) (ByteBuffer.wrap(new byte[]{length_high, length_low}).getShort() + 3);
        byte[] sizeArray = ByteBuffer.allocate(2).putShort(size).array();
        builder.write(writeCharacteristic, new byte[]{FitProConstants.DATA_HEADER_ACK, 0, 5, command_group, 1, sizeArray[0], sizeArray[1], 1});
        builder.queue(getQueue());
    }

    @Override
    public void onTestNewFunction() {
        LOG.debug("Hello FitPro Test function");
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {
        LOG.debug("FitPro send weather");
        short todayMax = (short) (weatherSpec.todayMaxTemp - 273);
        short todayMin = (short) (weatherSpec.todayMinTemp - 273);
        byte weatherUnit = 0;
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            todayMax = (short) (todayMax * 1.8f + 32);
            todayMin = (short) (todayMin * 1.8f + 32);
            weatherUnit = 1;
        }

        byte currentConditionCode = Weather.mapToFitProCondition(weatherSpec.currentConditionCode);
        TransactionBuilder builder = new TransactionBuilder("weather");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_WEATHER, new byte[]{(byte) todayMin, (byte) todayMax, (byte) currentConditionCode, (byte) weatherUnit}));
        builder.queue(getQueue());
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        LOG.debug("FitPro notification: " + notificationSpec.type);
        TransactionBuilder builder = new TransactionBuilder("notification");
        byte icon = NOTIFICATION_ICON_SMS;
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                icon = NOTIFICATION_ICON_SMS;
                break;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                icon = NOTIFICATION_ICON_FACEBOOK;
                break;
            case LINE:
                icon = NOTIFICATION_ICON_LINE;
                break;
            case WHATSAPP:
                icon = NOTIFICATION_ICON_WHATSAPP;
                break;
            case TWITTER:
                icon = NOTIFICATION_ICON_TWITTER;
                break;
            case SIGNAL:
            case VIBER:
            case CONVERSATIONS:
                icon = NOTIFICATION_ICON_QQ;
                break;
            case WECHAT:
            case GMAIL:
                icon = NOTIFICATION_ICON_WECHAT;
                break;
            case INSTAGRAM:
                icon = NOTIFICATION_ICON_INSTAGRAM;
                break;
            default:
                icon = NOTIFICATION_ICON_SMS;
                break;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(icon);
            outputStream.write(0x0);
            outputStream.write(0x0);

            if (notificationSpec.sender != null) {
                outputStream.write(notificationSpec.sender.getBytes(StandardCharsets.UTF_8));
                outputStream.write(0x20);
            } else {
                if (notificationSpec.phoneNumber != null) { //use number only if there is no sender
                    outputStream.write(notificationSpec.phoneNumber.getBytes(StandardCharsets.UTF_8));
                    outputStream.write(0x20);
                }
            }

            if (notificationSpec.subject != null) {
                outputStream.write(notificationSpec.subject.getBytes(StandardCharsets.UTF_8));
                outputStream.write(0x20);
            }
            if (notificationSpec.body != null) {
                outputStream.write(notificationSpec.body.getBytes(StandardCharsets.UTF_8));
                outputStream.write(0x20);
            }

        } catch (IOException e) {
            LOG.error("FitPro error sending notification: " + e);
        }
        String output = outputStream.toString();
        if (outputStream.toString().length() > 60) {
            output = outputStream.toString().substring(0, 60);
        }

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATION_MESSAGE, output.getBytes(StandardCharsets.UTF_8)));
        builder.queue(getQueue());
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    public FitProDeviceSupport setLanguage(TransactionBuilder builder) {
        LOG.debug("FitPro set language");
        String localeString = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("language", "auto");
        if (localeString == null || localeString.equals("auto")) {
            String language = Locale.getDefault().getLanguage();
            String country = Locale.getDefault().getCountry();

            if (country == null) {
                country = language;
            }
            localeString = language + "_" + country.toUpperCase();
        }
        LOG.info("Setting device to locale: " + localeString);

        byte languageCode = FitProConstants.LANG_ENGLISH;

        switch (localeString.substring(0, 2)) {
            case "zh":
                languageCode = FitProConstants.LANG_CHINESE;
                break;
            case "it":
                languageCode = FitProConstants.LANG_ITALIAN;
                break;
            case "cs":
                languageCode = FitProConstants.LANG_CZECH;
                break;
            case "en":
                languageCode = FitProConstants.LANG_ENGLISH;
                break;
            case "tr":
                languageCode = FitProConstants.LANG_TURKISH;
                break;
            case "ru":
                languageCode = FitProConstants.LANG_RUSSIAN;
                break;
            case "pl":
                languageCode = FitProConstants.LANG_POLISH;
                break;
            case "nl":
                languageCode = FitProConstants.LANG_NETHERLANDS;
                break;
            case "fr":
                languageCode = FitProConstants.LANG_FRENCH;
                break;
            case "es":
                languageCode = FitProConstants.LANG_SPANISH;
                break;
            case "de":
                languageCode = FitProConstants.LANG_GERMAN;
                break;
            case "pt":
                languageCode = FitProConstants.LANG_PORTUGUESE;
                break;

            default:
                languageCode = FitProConstants.LANG_ENGLISH;
        }

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_LANGUAGE, languageCode));

        return this;
    }

    public FitProDeviceSupport setUserData(TransactionBuilder builder) {
        //0xcd 0x00 0x09 0x12 0x01 0x04 0x00 0x04 0xaf 0x59 0x09 0xe1 FitPro
        LOG.debug("FitPro set user data");

        ActivityUser activityUser = new ActivityUser();

        int age = activityUser.getAge();

        int gender = activityUser.getGender();
        byte genderUnit = GENDER_FEMALE;
        if (gender == ActivityUser.GENDER_MALE) {
            genderUnit = GENDER_MALE;
        }

        int heightCm = activityUser.getHeightCm();
        int weightKg = activityUser.getWeightKg();

        byte distanceUnit = UNIT_METRIC;
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            distanceUnit = UNIT_IMPERIAL;
        }

        int userData = genderUnit << 31 | age << 24 | heightCm << 15 | weightKg << 5 | distanceUnit;
        byte[] data = craftData(CMD_GROUP_GENERAL, CMD_SET_USER_DATA, ByteBuffer.allocate(4).putInt(userData).array());
        builder.write(writeCharacteristic, data);
        return this;
    }

    @Override
    public void onFetchRecordedData(int dataTypes) {
        indicateFinishedFetchingOperation();
        TransactionBuilder builder = new TransactionBuilder("fetch data1");
        builder.add(new SetDeviceBusyAction(getDevice(), getContext().getString(R.string.busy_task_fetch_activity_data), getContext()));
        builder.write(writeCharacteristic, craftData(CMD_GROUP_RECEIVE_SPORTS_DATA, CMD_REQUEST_STEPS_DATA1, VALUE_ON));
        builder.queue(getQueue());
    }


    public void handleDayTotalsData(byte[] value) {
        LOG.debug("FitPro handle day data length: " + value.length);
        debugPrintArray(value, "value");
        if (value.length < 10) {
            LOG.debug("FitPro payload too short");
            return;
        }
        debugPrintArray(value, "processing");
        int steps = ByteBuffer.wrap(value, 10, 4).getInt();
        int distance = ByteBuffer.wrap(value, 14, 4).getInt();

        byte[] caloriesBytes = new byte[3];
        System.arraycopy(value, 18, caloriesBytes, 0, 2);
        int calories = ByteBuffer.wrap(caloriesBytes, 0, 3).getShort();

        LOG.debug("processing day data summary, steps: " + steps + " distance: " + distance + " calories: " + calories);
        sendAck(value[3], value[1], value[2], value[5]);
        //handleDayTotalsData(steps, distance, calories);
    }

    public void handleBatteryInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo info) {
        LOG.debug("FitPro battery info: " + info);
        batteryCmd.level = (short) info.getPercentCharged();
        handleGBDeviceEvent(batteryCmd);
    }

    /*   public void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
           LOG.debug("FitPro device info: " + info);
           versionCmd.hwVersion = "+FitPro";
           versionCmd.fwVersion = info.getFirmwareRevision();
           handleGBDeviceEvent(versionCmd);
       }

     */
    public void handleCamera(byte command) {
        GB.toast(getContext(), "Camera buttons are detected but not further handled.", Toast.LENGTH_SHORT, GB.INFO);
    }

    public void handleFindPhone() {
        LOG.info("FitPro find phone");
        GBDeviceEventFindPhone deviceEventFindPhone = new GBDeviceEventFindPhone();
        deviceEventFindPhone.event = GBDeviceEventFindPhone.Event.START;
        evaluateGBDeviceEvent(deviceEventFindPhone);
    }

    public void handleMediaButton(byte command) {
        GBDeviceEventMusicControl deviceEventMusicControl = new GBDeviceEventMusicControl();
        if (command == RX_MEDIA_PLAY_PAUSE) {
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PLAYPAUSE;
            evaluateGBDeviceEvent(deviceEventMusicControl);
        } else if (command == RX_MEDIA_FORW) {
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.NEXT;
            evaluateGBDeviceEvent(deviceEventMusicControl);
        } else if (command == RX_MEDIA_BACK) {
            deviceEventMusicControl.event = GBDeviceEventMusicControl.Event.PREVIOUS;
            evaluateGBDeviceEvent(deviceEventMusicControl);
        }
    }

    public FitProDeviceSupport setVibrations(TransactionBuilder builder) {
        LOG.debug("FitPro set enable vibrations");
        boolean vibrations = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_VIBRATION_ENABLE, false);
        byte[] enable = VALUE_SET_DEVICE_VIBRATIONS_ENABLE;
        if (!vibrations) {
            enable = VALUE_SET_DEVICE_VIBRATIONS_DISABLE;
        }
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_DEVICE_VIBRATIONS, enable));
        return this;
    }

    public void debugPrintArray(byte[] bytes, String label) {
        if (!debugEnabled) return;
        String arrayString = nodomain.freeyourgadget.gadgetbridge.util.ArrayUtils.arrayToString(bytes);
        LOG.debug("FitPro debug print " + label + ": " + arrayString);
    }

    public FitProDeviceSupport setNotifications(TransactionBuilder builder) {
        LOG.debug("FitPro set enable notifications");
        boolean notifications = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean(DeviceSettingsPreferenceConst.PREF_NOTIFICATION_ENABLE, false);
        byte[] enable = VALUE_SET_NOTIFICATIONS_ENABLE_ON;
        if (!notifications) {
            enable = VALUE_SET_NOTIFICATIONS_ENABLE_OFF;
        }
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_NOTIFICATIONS_ENABLE, enable));
        return this;
    }

    public byte[] craftData(byte command_group, byte command, byte value) {
        return craftData(command_group, command, new byte[]{value});
    }

    public byte[] craftData(byte command_group, byte command) {
        return craftData(command_group, command, new byte[]{});
    }

    @Override
    public void onSetTime() {
        LOG.debug("FitPro set date and time");
        TransactionBuilder builder = new TransactionBuilder("Set date and time");
        setTime(builder);
        builder.queue(getQueue());
    }


    public FitProDeviceSupport setTime(TransactionBuilder builder) {
        LOG.debug("FitPro set time");
        Calendar calendar = Calendar.getInstance();

        int datetime = calendar.get(Calendar.SECOND) | (
                (calendar.get(Calendar.YEAR) - 2000) << 26 | calendar.get(Calendar.MONTH) + 1 << 22 |
                        calendar.get(Calendar.DAY_OF_MONTH) << 17 |
                        calendar.get(Calendar.HOUR_OF_DAY) << 12 | calendar.get(Calendar.MINUTE) << 6);

        //this is how the values can be re-stored
        // result is this
        //byte[] array = new byte[]{(byte) (datetime >> 24), (byte) (datetime >> 16), (byte) (datetime >> 8), (byte) (datetime >> 0)};
        // int datetime2 = ByteBuffer.wrap(array).getInt();

        //byte[] time = craftData(LT716Constants.CMD_SET_DATE_TIME, new byte[]{(byte) (datetime >> 24), (byte) (datetime >> 16), (byte) (datetime >> 8), (byte) (datetime >> 0)});
        byte[] time = craftData(CMD_GROUP_GENERAL, CMD_SET_DATE_TIME, (ByteBuffer.allocate(4).putInt(datetime).array()));
        builder.write(writeCharacteristic, time);
        return this;
    }


    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        LOG.debug("FitPro set alarms");

        // handle one-shot alarm from the widget:
        // this device doesn't have concept of on-off alarm, so use the last slot for this and store
        // this alarm in the database so the user knows what is going on and can disable it

        if (alarms.toArray().length == 1 && alarms.get(0).getRepetition() == 0) {
            Alarm oneshot = alarms.get(0);
            DBHandler db = null;
            try {
                db = GBApplication.acquireDB();
                DaoSession daoSession = db.getDaoSession();
                Device device = DBHelper.getDevice(gbDevice, daoSession);
                User user = DBHelper.getUser(daoSession);
                nodomain.freeyourgadget.gadgetbridge.entities.Alarm tmpAlarm =
                        new nodomain.freeyourgadget.gadgetbridge.entities.Alarm(
                                device.getId(),
                                user.getId(),
                                7,
                                true,
                                false,
                                false,
                                0,
                                oneshot.getHour(),
                                oneshot.getMinute(),
                                true, //kind of indicate the specialty of this alarm
                                "",
                                "");
                daoSession.insertOrReplace(tmpAlarm);
                GBApplication.releaseDB();
            } catch (GBException e) {
                LOG.error("error storing one shot quick alarm");
            }
        }

        try {
            TransactionBuilder builder = performInitialized("Set alarm");
            boolean anyAlarmEnabled = false;
            byte[] all_alarms = new byte[]{};

            for (Alarm alarm : alarms) {
                Calendar calendar = AlarmUtils.toCalendar(alarm);
                anyAlarmEnabled |= alarm.getEnabled();
                LOG.debug("alarms: " + alarm.getPosition());
                int maxAlarms = 8;
                if (alarm.getPosition() >= maxAlarms) { //we should never encounter this, but just in case
                    if (alarm.getEnabled()) {
                        GB.toast(getContext(), "Only 8 alarms are supported.", Toast.LENGTH_LONG, GB.WARN);
                    }
                    return;
                }
                if (alarm.getEnabled()) {
                    long datetime = (long) alarm.getRepetition() | (
                            (long) (calendar.get(Calendar.YEAR) - 2000) << 34 |
                                    (long) (calendar.get(Calendar.MONTH) + 1) << 30 |
                                    (long) (calendar.get(Calendar.DAY_OF_MONTH)) << 25 |
                                    (long) (calendar.get(Calendar.HOUR_OF_DAY)) << 20 |
                                    (long) (calendar.get(Calendar.MINUTE)) << 14 |
                                    1L << 11);
                    byte[] single_alarm = new byte[]{(byte) (datetime >> 32), (byte) (datetime >> 24), (byte) (datetime >> 16), (byte) (datetime >> 8), (byte) (datetime)};
                    all_alarms = ArrayUtils.addAll(all_alarms, single_alarm);
                }
            }

            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_ALARM, all_alarms));
            builder.queue(getQueue());
            if (anyAlarmEnabled) {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_ok), Toast.LENGTH_SHORT, GB.INFO);
            } else {
                GB.toast(getContext(), getContext().getString(R.string.user_feedback_all_alarms_disabled), Toast.LENGTH_SHORT, GB.INFO);
            }
        } catch (IOException ex) {
            GB.toast(getContext(), getContext().getString(R.string.user_feedback_miband_set_alarms_failed), Toast.LENGTH_LONG, GB.ERROR, ex);
        }
    }

    @Override
    public void onReset(int flags) {
        LOG.debug("FitPro reset flags: " + flags);
        byte[] command = craftData(CMD_GROUP_RESET, CMD_RESET);
        switch (flags) {
            case 1:
                command = craftData(CMD_GROUP_RESET, CMD_RESET);
                break;
            case 2:
                command = craftData(CMD_GROUP_BIND, CMD_UNBIND);
                break;
        }

        getQueue().clear();
        TransactionBuilder builder = new TransactionBuilder("resetting");
        builder.write(writeCharacteristic, command);
        builder.queue(getQueue());
    }

    @Override
    public void onHeartRateTest() {
        TransactionBuilder builder = new TransactionBuilder("notification");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_HEART_RATE_MEASUREMENT, VALUE_ON));
        builder.queue(getQueue());
    }

    @Override
    public void onEnableRealtimeHeartRateMeasurement(boolean enable) {

    }

    @Override
    public void onFindDevice(boolean start) {
        getQueue().clear();
        LOG.debug("FitPro find device");
        TransactionBuilder builder = new TransactionBuilder("searching");
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_FIND_BAND, start ? VALUE_ON : VALUE_OFF));
        builder.queue(getQueue());
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


    public FitProDeviceSupport setAutoHeartRate(TransactionBuilder builder) {
        LOG.debug("FitPro set automatic heartrate measurements");
        boolean prefAutoheartrateSwitch = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("pref_autoheartrate_switch", false);
        LOG.info("Setting autoheartrate to " + prefAutoheartrateSwitch);

        boolean sleep = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("pref_autoheartrate_sleep", false);
        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_autoheartrate_start", "06:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_autoheartrate_end", "23:00");
        String interval = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_autoheartrate_interval", "2");

        int intervalInt = Integer.parseInt(interval);
        int sleepInt = sleep ? 1 : 0;
        int autoheartrateInt = prefAutoheartrateSwitch ? 1 : 0;

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(autoheartrateInt);
        outputStream.write(sleepInt);
        outputStream.write(intervalInt >> 8);
        outputStream.write(intervalInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);
        //outputStream.write(0x7F);

        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_GROUP_HEARTRATE_SETTINGS, outputStream.toByteArray()));

        return this;
    }

    public FitProDeviceSupport setLongSitReminder(TransactionBuilder builder) {
        LOG.debug("FitPro set inactivity warning");
        boolean prefLongsitSwitch = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getBoolean("pref_longsit_switch", false);
        LOG.info("Setting long sit warning to " + prefLongsitSwitch);

        if (prefLongsitSwitch) {

            String inactivity = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_longsit_period", "4");
            String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_longsit_start", "08:00");
            String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("pref_longsit_end", "16:00");
            Calendar startCalendar = GregorianCalendar.getInstance();
            Calendar endCalendar = GregorianCalendar.getInstance();
            DateFormat df = new SimpleDateFormat("HH:mm");

            try {
                startCalendar.setTime(df.parse(start));
                endCalendar.setTime(df.parse(end));
            } catch (ParseException e) {
                LOG.debug("settings error: " + e);
            }

            int inactivityInt = Integer.parseInt(inactivity);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(VALUE_SET_LONG_SIT_REMINDER_ON);
                outputStream.write(inactivityInt);
                outputStream.write(startCalendar.get(Calendar.HOUR_OF_DAY));
                outputStream.write(endCalendar.get(Calendar.HOUR_OF_DAY));
                outputStream.write(0x7F);
            } catch (IOException e) {
                LOG.error("settings error: " + e);
            }

            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_LONG_SIT_REMINDER, outputStream.toByteArray()));
            LOG.info("Setting long sit warning to scheduled");

        } else {
            builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_LONG_SIT_REMINDER, VALUE_SET_LONG_SIT_REMINDER_OFF));
            LOG.info("Setting long sit warning to OFF");
        }
        return this;
    }

    public FitProDeviceSupport setDoNotDisturb(TransactionBuilder builder) {
        LOG.debug("FitPro set DND");
        String dnd = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("do_not_disturb_no_auto", "off");
        LOG.info("Setting DND to " + dnd);
        int dndInt = dnd.equals("scheduled") ? 1 : 0;

        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("do_not_disturb_no_auto_start", "22:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("do_not_disturb_no_auto_end", "06:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        outputStream.write(dndInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);

        debugPrintArray(craftData(CMD_GROUP_GENERAL, CMD_DND, outputStream.toByteArray()), "enable DND");
        debugPrintArray(outputStream.toByteArray(), "payload");
        LOG.info("Setting DND to scheduled: " + start + " " + end);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_DND, outputStream.toByteArray()));
        LOG.info("Setting DND scheduled");

        return this;
    }

    public FitProDeviceSupport setSleepTime(TransactionBuilder builder) {
        LOG.debug("FitPro set sleep times");
        String sleepTime = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("prefs_enable_sleep_time", "off");
        LOG.info("Setting sleep times to " + sleepTime);
        int sleepTimeInt = sleepTime.equals("scheduled") ? 1 : 0;

        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("prefs_sleep_time_start", "22:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("prefs_sleep_time_end", "06:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(sleepTimeInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);
        debugPrintArray(craftData(CMD_GROUP_GENERAL, CMD_SET_SLEEP_TIMES, outputStream.toByteArray()), "enable sleep time");
        debugPrintArray(outputStream.toByteArray(), "payload");
        LOG.info("Setting sleep times scheduled: " + start + " " + end);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_SLEEP_TIMES, outputStream.toByteArray()));
        LOG.info("Setting sleep times scheduled");
        return this;
    }

    public FitProDeviceSupport setWearLocation(TransactionBuilder builder) {
        LOG.debug("FitPro set wearing location");
        byte location = VALUE_SET_ARM_LEFT;
        String setLocation = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_WEARLOCATION, "left");
        if ("right".equals(setLocation)) {
            location = VALUE_SET_ARM_RIGHT;
        }
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_ARM, location));
        return this;
    }

    public FitProDeviceSupport setDisplayOnLift(TransactionBuilder builder) {
        LOG.debug("FitPro set display on lift");
        String displayLift = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("activate_display_on_lift_wrist", "off");

        int displayLiftInt = displayLift.equals("scheduled") ? 1 : 0;

        LOG.info("Setting activate display on lift wrist to:" + displayLift + ": " + displayLiftInt);

        String start = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("display_on_lift_start", "08:00");
        String end = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString("display_on_lift_end", "16:00");

        Calendar startCalendar = GregorianCalendar.getInstance();
        Calendar endCalendar = GregorianCalendar.getInstance();
        DateFormat df = new SimpleDateFormat("HH:mm");

        try {
            startCalendar.setTime(df.parse(start));
            endCalendar.setTime(df.parse(end));
        } catch (ParseException e) {
            LOG.error("settings error: " + e);
        }

        int startTime = (startCalendar.get(Calendar.HOUR_OF_DAY) * 60) + startCalendar.get(Calendar.MINUTE);
        int endTime = (endCalendar.get(Calendar.HOUR_OF_DAY) * 60) + endCalendar.get(Calendar.MINUTE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(displayLiftInt);
        outputStream.write(startTime >> 8);
        outputStream.write(startTime);
        outputStream.write(endTime >> 8);
        outputStream.write(endTime);
        debugPrintArray(craftData(CMD_GROUP_GENERAL, CMD_SET_DISPLAY_ON_LIFT, outputStream.toByteArray()), "enable lift display");
        debugPrintArray(outputStream.toByteArray(), "payload");
        LOG.info("Setting activate display on lift wrist scheduled: " + start + " " + end);
        builder.write(writeCharacteristic, craftData(CMD_GROUP_GENERAL, CMD_SET_DISPLAY_ON_LIFT, outputStream.toByteArray()));
        LOG.info("Setting activate display on lift wrist scheduled");

        return this;
    }

    public FitProDeviceSupport setStepsGoal(TransactionBuilder builder) {
        LOG.debug("FitPro set step goal");
        //cd 00 09 12 01 03 00 04 00 00 05 dc

        ActivityUser activityUser = new ActivityUser();
        int stepGoal = activityUser.getStepsGoal();
        byte[] data = craftData(CMD_GROUP_GENERAL, CMD_SET_STEP_GOAL, ByteBuffer.allocate(4).putInt(stepGoal).array());

        builder.write(writeCharacteristic, data);
        return this;
    }

    public void handleSleepData(byte[] value) {
        debugPrintArray(value, "sleep data value");
        // sleep packet consists of: date + list of 4bytes of 15minutes intervals
        // these intervals contain seconds offset from the date and type of sleep
        byte[] dateArray = new byte[2];
        System.arraycopy(value, 8, dateArray, 0, 2);
        Calendar date = decodeDateTime(dateArray);
        List<FitProActivitySample> samples = new ArrayList<>();
        for (int i = 12; i < value.length - 3; i = i + 4) {
            byte[] packet = new byte[4];
            System.arraycopy(value, i, packet, 0, 4);
            int data = ByteBuffer.wrap(packet).getInt();
            int activity_kind = (int) (data & 0xff);
            int encodedTime = (int) (data >> 16);
            int seconds = getSleepSecondsOfDay(encodedTime);
            Calendar now = (Calendar) date.clone(); // do not modify the caller's argument
            now.add(Calendar.SECOND, seconds);
            int timestamp = (int) (now.getTimeInMillis() / 1000L);
            debugPrintArray(packet, "processing sleep packet");

            LOG.debug("FitPro new sleep: " + activity_kind + " seconds: " + seconds + " ts: " + timestamp + " date: " + DateTimeUtils.formatDateTime(new Date(timestamp * 1000L)));

            FitProActivitySample sample = new FitProActivitySample();
            sample.setTimestamp(timestamp);
            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setActiveTimeMinutes(15);
            sample.setRawKind(rawSleepKindToUniqueKind(activity_kind));
            samples.add(sample);
        }
        if (addGBActivitySamples(samples)) {
            sendAck(value[3], value[1], value[2], value[5]);
        }
    }

    public int rawSleepKindToUniqueKind(int kind) {
        //step and sleep are the same kind so we must distinguish them
        return kind + 10;
    }

    public int rawActivityKindToUniqueKind(int kind) {
        return kind;
    }

    public void handleStepData(byte[] value) {
        debugPrintArray(value, "step data value");
        // step packet consists of: date + list of 8bytes of (always?) 5minutes intervals
        // these intervals contain seconds offset from the date, type of activity, calories,
        // steps, distance, duration

        byte[] dateArray = new byte[2];
        System.arraycopy(value, 8, dateArray, 0, 2);
        Calendar date = decodeDateTime(dateArray);
        List<FitProActivitySample> samples = new ArrayList<>();
        for (int i = 12; i < value.length - 7; i = i + 8) {
            byte[] packet = new byte[8];
            System.arraycopy(value, i, packet, 0, 8);
            long data = ByteBuffer.wrap(packet).getLong();
            int steps = (int) Math.abs((data >> 52));
            int calories = (int) (data & 0x7ffff);
            int activity_kind = (int) ((data >> 19) & 0x1);
            int duration = (int) ((data >> 48) & 0xf);
            int distance = (int) ((data >> 32) & 0xffff);
            int encodedTime = (int) ((data >> 21) & 0x7ff);
            int seconds = getSecondsOfDay(encodedTime);
            Calendar now = (Calendar) date.clone(); // do not modify the caller's argument
            now.add(Calendar.SECOND, seconds);
            int timestamp = (int) (now.getTimeInMillis() / 1000L);
            debugPrintArray(packet, "processing steps packet");
            LOG.debug("FitPro adding new steps: " + steps);
            FitProActivitySample sample = new FitProActivitySample();
            sample.setTimestamp(timestamp);
            sample.setHeartRate(ActivitySample.NOT_MEASURED);
            sample.setSteps(steps);
            sample.setDistanceMeters(distance);
            sample.setCaloriesBurnt(calories);
            sample.setActiveTimeMinutes(duration);
            sample.setRawKind(rawActivityKindToUniqueKind(activity_kind));
            samples.add(sample);
        }
        if (addGBActivitySamples(samples)) {
            sendAck(value[3], value[1], value[2], value[5]);
        }
    }

    public void addGBActivitySample(FitProActivitySample sample) {
        List<FitProActivitySample> samples = new ArrayList<>();
        samples.add(sample);
        addGBActivitySamples(samples);
    }

    private boolean addGBActivitySamples(List<FitProActivitySample> samples) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            User user = DBHelper.getUser(dbHandler.getDaoSession());
            Device device = DBHelper.getDevice(this.getDevice(), dbHandler.getDaoSession());
            FitProSampleProvider provider = new FitProSampleProvider(this.getDevice(), dbHandler.getDaoSession());

            for (FitProActivitySample sample : samples) {
                sample.setDevice(device);
                sample.setUser(user);
                sample.setProvider(provider);
                provider.addGBActivitySample(sample);
            }

        } catch (Exception ex) {
            LOG.error("Error saving samples: " + ex);
            GB.updateTransferNotification(null, "Data transfer failed", false, 0, getContext());
            return false;
        }
        return true;
    }

    public void broadcastSample(FitProActivitySample sample) {
        Intent intent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES)
                .putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    public void handleHR(int seconds, int heartRate, int pressureLow, int pressureHigh, int spo2) {
        LOG.debug("FitPro handle heart rate measurement");

        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.add(Calendar.SECOND, seconds);
        LOG.debug("date: " + date);

        FitProActivitySample sample = new FitProActivitySample();

        sample.setHeartRate(heartRate);
        sample.setPressureLowMmHg(pressureLow);
        sample.setPressureHighMmHg(pressureHigh);
        sample.setSpo2Percent(spo2);

        sample.setTimestamp((int) (date.getTimeInMillis() / 1000));
        sample.setRawKind(ActivityKind.TYPE_ACTIVITY);

        addGBActivitySample(sample);
        broadcastSample(sample);
        GB.signalActivityDataFinish();
    }

    public void handleDayTotalsData(int steps, int distance, int calories) {
        //this is for day data values, not used in Gb, handleStepData uses the better, 5min data
        LOG.debug("FitPro handle day total steps");

        LOG.debug("Steps: " + steps);
        LOG.debug("Distance: " + distance);
        LOG.debug("Calories: " + calories);

        Calendar dateStart = Calendar.getInstance();
        dateStart.set(Calendar.HOUR_OF_DAY, 0);
        dateStart.set(Calendar.MINUTE, 0);
        dateStart.set(Calendar.SECOND, 0);

        Calendar dateEnd = Calendar.getInstance();
        dateEnd.set(Calendar.HOUR_OF_DAY, 23);
        dateEnd.set(Calendar.MINUTE, 59);
        dateEnd.set(Calendar.SECOND, 59);

        int dayStepCount = getStepsOnDay(dateStart, dateEnd);
        int newSteps = (steps - dayStepCount);
        LOG.debug("FitPro dayStepCount " + dayStepCount);
        LOG.debug("FitPro new steps " + newSteps);

        /*
        if (newSteps > 0) {
            LOG.debug("FitPro adding new steps " + newSteps);
            ShenTechActivitySample sample = new ShenTechActivitySample();
            Calendar date = Calendar.getInstance();
            sample.setTimestamp((int) (date.getTimeInMillis() / 1000));
            sample.setSteps(newSteps);
            sample.setDistanceMeters(distance);
            sample.setCaloriesBurnt(calories);
            sample.setRawKind(ActivityKind.TYPE_ACTIVITY);
            sample.setRawIntensity(1);
            addGBActivitySample(sample);
            broadcastSample(sample);
        }
         */
    }

    private int getStepsOnDay(Calendar dayStart, Calendar dayEnd) {
        //this is for day data values, not used in Gb, handleStepData uses 5min data which is better
        try (DBHandler dbHandler = GBApplication.acquireDB()) {

            FitProSampleProvider provider = new FitProSampleProvider(this.getDevice(), dbHandler.getDaoSession());

            List<FitProActivitySample> samples = provider.getActivitySamples(
                    (int) (dayStart.getTimeInMillis() / 1000L),
                    (int) (dayEnd.getTimeInMillis() / 1000L));

            int totalSteps = 0;

            for (FitProActivitySample sample : samples) {
                totalSteps += sample.getSteps();
            }

            return totalSteps;

        } catch (Exception ex) {
            LOG.error(ex.getMessage());
            return 0;
        }
    }

    public int getSecondsOfDay(int encodedTime) {
        int hours = (int) Math.floor((encodedTime * 15) / 60);
        int minutes = (encodedTime * 15) % 60;
        int seconds = (hours * 3600) + (minutes * 60);
        return seconds;
    }

    public int getSleepSecondsOfDay(int encodedTime) {
        int hours = (int) Math.floor(encodedTime / 60);
        int minutes = encodedTime % 60;
        int seconds = (hours * 3600) + (minutes * 60);
        return seconds;
    }

    public Calendar decodeDateTime(byte[] dateArray) {
        debugPrintArray(dateArray, "array to decode to date time");
        short dateShort = ByteBuffer.wrap(dateArray).getShort();

        int day = (dateShort & 0x1f);
        int month = ((dateShort >> 5) & 0xf);
        int year = ((dateShort >> 9) + 2000);

        Calendar date = GregorianCalendar.getInstance();
        date.set(year, month - 1, day, 0, 0, 0);
        return date;
    }
}