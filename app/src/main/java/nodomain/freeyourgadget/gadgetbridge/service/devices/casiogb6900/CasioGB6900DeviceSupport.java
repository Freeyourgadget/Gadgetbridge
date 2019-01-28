/*      Copyright (C) 2018 Andreas Böhler
        based on code from BlueWatcher, https://github.com/masterjc/bluewatcher

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casiogb6900;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.devices.casiogb6900.CasioGB6900Constants;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class CasioGB6900DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGB6900DeviceSupport.class);

    private ArrayList<BluetoothGattCharacteristic> mCasioCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
    private CasioGATTThread mThread;
    private CasioHandlerThread mHandlerThread = null;
    private MusicSpec mBufferMusicSpec = null;
    private MusicStateSpec mBufferMusicStateSpec = null;
    private BluetoothGatt mBtGatt = null;

    public CasioGB6900DeviceSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_IMMEDIATE_ALERT);
        addSupportedService(CasioGB6900Constants.CASIO_VIRTUAL_SERVER_SERVICE);
        addSupportedService(CasioGB6900Constants.ALERT_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.CASIO_IMMEDIATE_ALERT_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.CURRENT_TIME_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.WATCH_CTRL_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.WATCH_FEATURES_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.CASIO_PHONE_ALERT_STATUS_SERVICE);
        addSupportedService(CasioGB6900Constants.MORE_ALERT_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.TX_POWER_SERVICE_UUID);
        addSupportedService(CasioGB6900Constants.LINK_LOSS_SERVICE);
        mThread = new CasioGATTThread(getContext(), this);
    }

    @Override
    public void setContext(GBDevice gbDevice, BluetoothAdapter btAdapter, Context context) {
        super.setContext(gbDevice, btAdapter, context);
        mThread.setContext(context);
        mThread.start();
    }

    @Override
    public void dispose() {
        LOG.info("Dispose");
        close();

        super.dispose();
    }

    private void close() {
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread.interrupt();
            mHandlerThread = null;
        }

        if(mThread != null) {
            mThread.quit();
            mThread.interrupt();
            mThread = null;
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        mBtGatt = gatt;
        super.onServicesDiscovered(gatt);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        gbDevice.setState(GBDevice.State.INITIALIZING);
        gbDevice.sendDeviceUpdateIntent(getContext());

        addCharacteristics();

        builder.setGattCallback(this);

        enableNotifications(builder, true);

        configureWatch(builder);

        LOG.info("Initialization Done");

        return builder;
    }

    // FIXME: Replace hardcoded values by configuration
    private void configureWatch(TransactionBuilder builder) {
        if (mBtGatt == null)
            return;

        byte value[] = new byte[]{GattCharacteristic.MILD_ALERT};

        BluetoothGattService llService = mBtGatt.getService(CasioGB6900Constants.LINK_LOSS_SERVICE);
        BluetoothGattCharacteristic charact = llService.getCharacteristic(CasioGB6900Constants.ALERT_LEVEL_CHARACTERISTIC_UUID);
        builder.write(charact, value);
    }

    private void addCharacteristics() {
        mCasioCharacteristics.clear();
        mCasioCharacteristics.add(getCharacteristic(CasioGB6900Constants.CASIO_A_NOT_COM_SET_NOT));
        mCasioCharacteristics.add(getCharacteristic(CasioGB6900Constants.CASIO_A_NOT_W_REQ_NOT));
        mCasioCharacteristics.add(getCharacteristic(CasioGB6900Constants.ALERT_LEVEL_CHARACTERISTIC_UUID));
        mCasioCharacteristics.add(getCharacteristic(CasioGB6900Constants.RINGER_CONTROL_POINT));
    }

    public boolean enableNotifications(TransactionBuilder builder, boolean enable) {
        for(BluetoothGattCharacteristic charact : mCasioCharacteristics) {
            builder.notify(charact, enable);
        }
        return true;
    }

    public void readTxPowerLevel() {
        try {
            TransactionBuilder builder = performInitialized("readTxPowerLevel");
            builder.read(getCharacteristic(CasioGB6900Constants.TX_POWER_LEVEL_CHARACTERISTIC_UUID));
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("readTxPowerLevel failed: " + e.getMessage());
        }
    }

    private void writeCasioCurrentTime(TransactionBuilder builder) {
        byte[] arr = new byte[10];
        Calendar cal = Calendar.getInstance();

        int year = cal.get(Calendar.YEAR);
        arr[0] = (byte)((year >>> 0) & 0xff);
        arr[1] = (byte)((year >>> 8) & 0xff);
        arr[2] = (byte)(1 + cal.get(Calendar.MONTH));
        arr[3] = (byte)cal.get(Calendar.DAY_OF_MONTH);
        arr[4] = (byte)cal.get(Calendar.HOUR_OF_DAY);
        arr[5] = (byte)cal.get(Calendar.MINUTE);
        arr[6] = (byte)(1 + cal.get(Calendar.SECOND));
        byte dayOfWk = (byte)(cal.get(Calendar.DAY_OF_WEEK) - 1);
        if(dayOfWk == 0)
            dayOfWk = 7;
        arr[7] = dayOfWk;
        arr[8] = (byte)(int) TimeUnit.MILLISECONDS.toSeconds(256 * cal.get(Calendar.MILLISECOND));
        arr[9] = 1; // or 0?

        BluetoothGattCharacteristic charact = getCharacteristic(CasioGB6900Constants.CURRENT_TIME_CHARACTERISTIC_UUID);
        if(charact != null) {
            charact.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            builder.write(charact, arr);
        }
        else {
            LOG.warn("Characteristic not found: CURRENT_TIME_CHARACTERISTIC_UUID");
        }
    }

    private void writeCasioLocalTimeInformation(TransactionBuilder builder) {
        Calendar cal = Calendar.getInstance();
        int zoneOffset = (int)TimeUnit.MILLISECONDS.toMinutes(cal.get(Calendar.ZONE_OFFSET));
        int dstOffset = (int)TimeUnit.MILLISECONDS.toMinutes(cal.get(Calendar.DST_OFFSET));
        byte byte0 = (byte)(zoneOffset / 15);
        byte byte1 = (byte)(dstOffset / 15);
        BluetoothGattCharacteristic charact = getCharacteristic(CasioGB6900Constants.LOCAL_TIME_CHARACTERISTIC_UUID);
        if(charact != null) {
            builder.write(charact, new byte[]{byte0, byte1});
        }
        else {
            LOG.warn("Characteristic not found: LOCAL_TIME_CHARACTERISTIC_UUID");
        }

    }

    private void writeCasioVirtualServerFeature(TransactionBuilder builder) {
        byte byte0 = (byte)0;
        byte0 |= 1; // Casio Current Time Service
        byte0 |= 2; // Casio Alert Notification Service
        byte0 |= 4; // Casio Phone Alert Status Service
        byte0 |= 8; // Casio Immediate Alert Service

        BluetoothGattCharacteristic charact = getCharacteristic(CasioGB6900Constants.CASIO_VIRTUAL_SERVER_FEATURES);
        if(charact != null) {
            builder.write(charact, new byte[]{byte0, 0x00});
        }
        else {
            LOG.warn("Characteristic not found: CASIO_VIRTUAL_SERVER_FEATURES");
        }
    }

    private boolean handleInitResponse(byte data) {
        boolean handled = false;
        switch(data)
        {
            case (byte) 1:
                LOG.info("Initialization done, setting state to INITIALIZED");
                if(mHandlerThread == null) {
                    mHandlerThread = new CasioHandlerThread(getDevice(), getContext(), this);
                }
                mHandlerThread.start();
                gbDevice.setState(GBDevice.State.INITIALIZED);
                gbDevice.sendDeviceUpdateIntent(getContext());
                handled = true;
                break;
            default:
                LOG.warn("handleInitResponse: Error initializing device, received unexpected value: " + data);
                gbDevice.setState(GBDevice.State.NOT_CONNECTED);
                gbDevice.sendDeviceUpdateIntent(getContext());
                handled = true;
                break;
        }
        return handled;
    }

    private boolean handleTimeRequests(byte data) {
        boolean handled = false;
        switch(data) // Request Type
        {
            case (byte) 1:
                try
                {
                    TransactionBuilder builder = createTransactionBuilder("writeCasioCurrentTime");
                    writeCasioCurrentTime(builder);
                    performConnected(builder.getTransaction());
                    handled = true;
                } catch (IOException e) {
                    LOG.warn("handleTimeRequests::writeCasioCurrentTime failed: " + e.getMessage());
                }
                break;
            case (byte) 2:
                try
                {
                    TransactionBuilder builder = createTransactionBuilder("writeCasioLocalTimeInformation");
                    writeCasioLocalTimeInformation(builder);
                    performConnected(builder.getTransaction());
                    handled = true;
                } catch (IOException e) {
                    LOG.warn("handleTimeRequests::writeCasioLocalTimeInformation failed: " + e.getMessage());
                }
                break;
        }
        return handled;
    }

    private boolean handleServerFeatureRequests(byte data) {
        try
        {
            TransactionBuilder builder = createTransactionBuilder("writeCasioVirtualServerFeature");
            writeCasioVirtualServerFeature(builder);
            performConnected(builder.getTransaction());
        } catch (IOException e) {
            LOG.warn("handleServerFeatureRequests failed: " + e.getMessage());
        }
        return true;
    }

    private boolean handleCasioCom(byte[] data) {
        boolean handled = false;

        if(data.length < 3) {
            LOG.warn("handleCasioCom failed: Received unexpected request (too short)");
            return false;
        }

        switch(data[0]) // ServiceID
        {
            case 0:
                handled = handleInitResponse(data[2]);
                break;
            case 2:
                handled = handleTimeRequests(data[2]);
                break;
            case 7:
                handled = handleServerFeatureRequests(data[2]);
                break;
        }
        return handled;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                       BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if(data.length == 0)
            return true;

        if(characteristicUUID.equals(CasioGB6900Constants.TX_POWER_LEVEL_CHARACTERISTIC_UUID)) {
            String str = "onCharacteristicRead: Received power level: ";
            for(int i=0; i<data.length; i++) {
                str += String.format("0x%1x ", data[i]);
            }
            LOG.info(str);
        }
        else {
            return super.onCharacteristicRead(gatt, characteristic, status);
        }

        return true;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        boolean handled = false;

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        if(characteristicUUID.equals(CasioGB6900Constants.CASIO_A_NOT_W_REQ_NOT)) {
            handled = handleCasioCom(data);
        }

        if(characteristicUUID.equals(CasioGB6900Constants.CASIO_A_NOT_COM_SET_NOT)) {
            handled = handleCasioCom(data);
        }

        if(characteristicUUID.equals(CasioGB6900Constants.ALERT_LEVEL_CHARACTERISTIC_UUID)) {
            GBDeviceEventFindPhone findPhoneEvent = new GBDeviceEventFindPhone();
            if(data[0] == 0x02) {
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.START;
            }
            else {
                findPhoneEvent.event = GBDeviceEventFindPhone.Event.STOP;
            }
                evaluateGBDeviceEvent(findPhoneEvent);
            handled = true;
        }

        if(characteristicUUID.equals(CasioGB6900Constants.RINGER_CONTROL_POINT)) {
            if(data[0] == 0x02)
            {
                LOG.info("Mute/ignore call event not yet supported by GB");
            }
            handled = true;
        }

        if(!handled) {
            LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
            return super.onCharacteristicChanged(gatt, characteristic);
        }
        return true;
    }

    private void showNotification(byte icon, String title, String message) {
        try {
            TransactionBuilder builder = performInitialized("showNotification");
            int len;

            byte[] titleBytes = title.getBytes(StandardCharsets.US_ASCII);
            len = titleBytes.length > 18 ? 18 : titleBytes.length;
            byte[] msg = new byte[2 + len];
            msg[0] = icon;
            msg[1] = 1;
            for(int i=0; i<len; i++)
            {
                msg[i + 2] = titleBytes[i];
            }

            builder.write(getCharacteristic(CasioGB6900Constants.ALERT_CHARACTERISTIC_UUID), msg);
            LOG.info("Showing notification, title: " + title + " message (not sent): " + message);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("showNotification failed: " + e.getMessage());
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
                icon = CasioGB6900Constants.SMS_NOTIFICATION_ID;
                break;
            case GENERIC_CALENDAR:
                icon = CasioGB6900Constants.CALENDAR_NOTIFICATION_ID;
                break;
            case GENERIC_EMAIL:
                icon = CasioGB6900Constants.MAIL_NOTIFICATION_ID;
                break;
            default:
                icon = CasioGB6900Constants.SNS_NOTIFICATION_ID;
                break;
        }
        showNotification(icon, notificationTitle, notificationSpec.body);
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        int alarmOffset = 4;
        byte[] data = new byte[20];
        for(int i=0; i<alarms.size(); i++)
        {
            Alarm alm = alarms.get(i);
            if(alm.getEnabled()) {
                data[i * alarmOffset] = 0x40;
            } else {
                data[i * alarmOffset] = 0;
            }
            if(alm.getRepetition(Alarm.ALARM_ONCE)) {
                data[i * alarmOffset] |= 0x20;
            }
            data[i * alarmOffset + 1] = 0;
            data[i * alarmOffset + 2] = (byte)alm.getHour();
            data[i * alarmOffset + 3] = (byte)alm.getMinute();
        }
        try {
            TransactionBuilder builder = performInitialized("setAlarm");
            builder.write(getCharacteristic(CasioGB6900Constants.CASIO_SETTING_FOR_ALM_CHARACTERISTIC_UUID), data);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.error("Error setting alarm: " + e.getMessage());
        }
    }

    @Override
    public void onSetTime() {
        try {
            TransactionBuilder builder = performInitialized("SetTime");
            writeCasioLocalTimeInformation(builder);
            writeCasioCurrentTime(builder);
            builder.queue(getQueue());
        } catch(IOException e) {
            LOG.warn("onSetTime failed: " + e.getMessage());
        }
    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(CasioGB6900Constants.CALL_NOTIFICATION_ID, callSpec.name, callSpec.number);
                break;
            default:
                LOG.info("not sending CallSpec since only CALL_INCOMING is handled");
                break;
        }
    }

    @Override
    public void onSetCannedMessages(CannedMessagesSpec cannedMessagesSpec) {

    }

    @Override
    public void onSetMusicState(MusicStateSpec stateSpec) {
        if(stateSpec != mBufferMusicStateSpec)
        {
            mBufferMusicStateSpec = stateSpec;
            sendMusicInfo();
        }
    }

    private void sendMusicInfo()
    {
        try {
            TransactionBuilder builder = performInitialized("sendMusicInfo");
            String info = "";
            if (mBufferMusicSpec.track != null && mBufferMusicSpec.track.length() > 0) {
                info += mBufferMusicSpec.track;
            }
            if (mBufferMusicSpec.album != null && mBufferMusicSpec.album.length() > 0) {
                info += mBufferMusicSpec.album;
            }
            if (mBufferMusicSpec.artist != null && mBufferMusicSpec.artist.length() > 0) {
                info += mBufferMusicSpec.artist;
            }
            byte[] bInfo = info.getBytes(StandardCharsets.US_ASCII);
            int len = bInfo.length > 17 ? 17 : bInfo.length;
            byte[] arr = new byte[len + 3];
            arr[0] = 0;
            arr[1] = 10;
            arr[2] = 1;
            for(int i=0; i<len; i++)
            {
                arr[i+3] = bInfo[i];
            }
            builder.write(getCharacteristic(CasioGB6900Constants.MORE_ALERT_FOR_LONG_UUID), arr);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("sendMusicInfo failed: " + e.getMessage());
        }
    }

    @Override
    public void onSetMusicInfo(MusicSpec musicSpec) {
        if(musicSpec != mBufferMusicSpec)
        {
            mBufferMusicSpec = musicSpec;
            sendMusicInfo();
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
        if(start) {
            showNotification(CasioGB6900Constants.SNS_NOTIFICATION_ID, "You found it!", "");
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
}
