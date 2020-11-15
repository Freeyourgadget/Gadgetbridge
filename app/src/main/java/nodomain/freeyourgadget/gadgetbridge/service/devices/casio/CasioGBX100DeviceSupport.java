/*  Copyright (C) 2018-2020 Andreas Böhler, Sebastian Kranz
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
package nodomain.freeyourgadget.gadgetbridge.service.devices.casio;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventCallControl;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventFindPhone;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;
import nodomain.freeyourgadget.gadgetbridge.devices.casio.CasioConstants;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.ServerTransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.devices.casio.operations.InitOperationGBX100;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class CasioGBX100DeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(CasioGBX100DeviceSupport.class);

    private final ArrayList<BluetoothGattCharacteristic> mCasioCharacteristics = new ArrayList<BluetoothGattCharacteristic>();
    private MusicSpec mBufferMusicSpec = null;
    private MusicStateSpec mBufferMusicStateSpec = null;
    private BluetoothGatt mBtGatt = null;
    private boolean mFirstConnect = false;

    public CasioGBX100DeviceSupport() {
        super(LOG);

        addSupportedService(CasioConstants.WATCH_FEATURES_SERVICE_UUID);
    }

    /*
    @Override
    public boolean connectFirstTime() {
        GB.toast(getContext(), "After first connect, disable and enable bluetooth on your Casio watch to really connect", Toast.LENGTH_SHORT, GB.INFO);
        mFirstConnect = true;
        return super.connect();
    }
     */

    public void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(gbDevice, GBDevice.State.INITIALIZED, getContext()));
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt) {
        mBtGatt = gatt;
        super.onServicesDiscovered(gatt);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        LOG.info("Initializing");

        try {
            new InitOperationGBX100(this, builder).perform();
        } catch (IOException e) {
            GB.toast(getContext(), "Initializing Casio watch failed", Toast.LENGTH_SHORT, GB.ERROR, e);
        }

        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");

        LOG.info("Initialization Done");

        return builder;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();

        if(data.length == 0)
            return true;

        return super.onCharacteristicRead(gatt, characteristic, status);
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        boolean handled = false;

        UUID characteristicUUID = characteristic.getUuid();
        byte[] data = characteristic.getValue();
        if (data.length == 0)
            return true;

        LOG.info("Unhandled characteristic change: " + characteristicUUID + " code: " + String.format("0x%1x ...", data[0]));
        return super.onCharacteristicChanged(gatt, characteristic);
    }

    @Override
    public boolean useAutoConnect() {
        return true;
    }

    private void showNotification(byte icon, String title, String message, int id) {
        byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        byte[] arr = new byte[22];
        arr[0] = (byte) 0x03; // (byte)(id & 0xff);
        arr[1] = (byte) 0x00; //((id >> 8) & 0xff);
        arr[2] = (byte) 0x00; // ((id >> 16) & 0xff);
        arr[3] = (byte) 0x00; // ((id >> 24) & 0xff);
        arr[4] = 0x00;
        arr[5] = (byte) 0x01;
        arr[6] = icon;
        arr[7] = (byte) 0x32;
        arr[8] = (byte) 0x30;
        arr[9] = (byte) 0x32;
        arr[10] = (byte) 0x30;
        arr[11] = (byte) 0x31;
        arr[12] = (byte) 0x31;
        arr[13] = (byte) 0x31;
        arr[14] = (byte) 0x33;
        arr[15] = (byte) 0x54;
        arr[16] = (byte) 0x30;
        arr[17] = (byte) 0x39;
        arr[18] = (byte) 0x33;
        arr[19] = (byte) 0x31;
        arr[20] = (byte) 0x35;
        arr[21] = (byte) 0x33;
        byte[] copy = Arrays.copyOf(arr, arr.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        // appName is currently not supported
        copy = Arrays.copyOf(copy, copy.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        if(titleBytes.length > 0) {
            copy = Arrays.copyOf(copy, copy.length + titleBytes.length);
            copy[copy.length-2-titleBytes.length] = (byte)(titleBytes.length & 0xff);
            copy[copy.length-1-titleBytes.length] = (byte)((titleBytes.length >> 8) & 0xff);
            System.arraycopy(titleBytes, 0, copy, copy.length - titleBytes.length, titleBytes.length);
        }
        copy = Arrays.copyOf(copy, copy.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        //subtitle is currently not supported
        copy = Arrays.copyOf(copy, copy.length + 2);
        copy[copy.length-2] = 0;
        copy[copy.length-1] = 0;
        if(messageBytes.length > 0) {
            copy = Arrays.copyOf(copy, copy.length + messageBytes.length);
            copy[copy.length-2-messageBytes.length] = (byte)(messageBytes.length & 0xff);
            copy[copy.length-1-messageBytes.length] = (byte)((messageBytes.length >> 8) & 0xff);
            System.arraycopy(messageBytes, 0, copy, copy.length - messageBytes.length, messageBytes.length);
        }
        for(int i=0; i<copy.length; i++) {
            copy[i] = (byte)(~copy[i]);
        }

        try {
            TransactionBuilder builder = performInitialized("showNotification");
            builder.write(getCharacteristic(CasioConstants.CASIO_NOTIFICATION_CHARACTERISTIC_UUID), copy);
            LOG.info("Showing notification, title: " + title + " message (not sent): " + message);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn("showNotification failed: " + e.getMessage());
        }
    }
    @Override
    public void onNotification(NotificationSpec notificationSpec) {
        String notificationTitle = StringUtils.getFirstOf(notificationSpec.sender, notificationSpec.title);
        byte icon;
        switch (notificationSpec.type) {
            case GENERIC_SMS:
                icon = CasioConstants.CATEGORY_EMAIL;
                break;
            case GENERIC_CALENDAR:
                icon = CasioConstants.CATEGORY_SCHEDULE_AND_ALARM;
                break;
            case GENERIC_EMAIL:
                icon = CasioConstants.CATEGORY_EMAIL;
                break;
            default:
                icon = CasioConstants.CATEGORY_SNS;
                break;
        }
        showNotification(icon, notificationTitle, notificationSpec.body, notificationSpec.getId());
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetTime() {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                showNotification(CasioConstants.CATEGORY_INCOMING_CALL, callSpec.name, callSpec.number, 0x9876);
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
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {

    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }
}
