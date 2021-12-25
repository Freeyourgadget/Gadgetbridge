/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Sebastian
    Kranz

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.domyos;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.net.Uri;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;

public class DomyosT540Support extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(DomyosT540Support.class);
    private static final UUID UUUD_SERVICE_DOMYOS = UUID.fromString("49535343-FE7D-4AE5-8FA9-9FAFD205E455");
    private static final UUID UUUD_CHARACTERISTICS_WRITE = UUID.fromString("49535343-8841-43F4-A8D4-ECBE34729BB3");
    private static final UUID UUUD_CHARACTERISTICS_NOTIFY = UUID.fromString("49535343-1E4D-4BD9-BA61-23C647249616");
    // those were captured during init, most of them seem to request information on the device, we leave it out for now
    //private static final byte[] COMMAND_UNKNOWN_INIT1 = new byte[]{(byte) 0xf0, (byte) 0xc9, (byte) 0xb9};
    //private static final byte[] COMMAND_UNKNOWN_INIT2 = new byte[]{(byte) 0xf0, (byte) 0xa3, (byte) 0x93};
    private static final byte[] COMMAND_STOP = new byte[]{(byte) 0xf0, (byte) 0xc8, (byte) 0x00, (byte) 0xb8};
    private static final byte[] COMMAND_START = new byte[]{(byte) 0xf0, (byte) 0xc8, (byte) 0x01, (byte) 0xb9};
    private static final byte[] COMMAND_SET_PARAMETERS = new byte[]{(byte) 0xf0, (byte) 0xad, (byte) 0xff, (byte) 0xff, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x01, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x00};
    private static final byte[] COMMAND_INIT_DISPLAY = new byte[]{(byte) 0xf0, (byte) 0xcb, 0x02, 0x00, 0x08, (byte) 0xff, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, (byte) 0xcc};

    //private static final byte[] COMMAND_UNKNOWN_INIT4 = new byte[]{(byte) 0xf0, (byte) 0xa4, (byte) 0x94};
    //private static final byte[] COMMAND_UNKNOWN_INIT5 = new byte[]{(byte) 0xf0, (byte) 0xa5, (byte) 0x95};
    //private static final byte[] COMMAND_UNKNOWN_INIT6 = new byte[]{(byte) 0xf0, (byte) 0xab, (byte) 0x9b};
    private static final byte[] COMMAND_REQUEST_DATA = new byte[]{(byte) 0xf0, (byte) 0xac, (byte) 0x9c};
    private final DeviceInfoProfile<DomyosT540Support> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final byte[] last_data;
    private int start_time = 0;
    private int last_time = 0;

    public DomyosT540Support() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(UUUD_SERVICE_DOMYOS);

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        IntentListener mListener = new IntentListener() {
            @Override
            public void notify(Intent intent) {
                String s = intent.getAction();
                if (s.equals(DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                    DeviceInfo deviceInfo = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
                    if (deviceInfo != null) {
                        handleDeviceInfo(deviceInfo);
                    }
                }
            }
        };
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
        last_data = new byte[26];
    }


    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        requestDeviceInfo(builder);
        enableNotifications(builder, true);
        setParameters(builder, 1.0f, 0, true);
        writeChunked(builder, COMMAND_INIT_DISPLAY);
        setInitialized(builder);
        writeChunked(builder, COMMAND_REQUEST_DATA);
        return builder;
    }

    private void requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
    }

    private void enableNotifications(TransactionBuilder builder, boolean enable) {
        builder.notify(getCharacteristic(UUUD_CHARACTERISTICS_NOTIFY), enable);
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    private void setDisplayValues(TransactionBuilder builder, int elapsedTime, int kCal, int heartRate, float incline, float speed, float distance) {
        ByteBuffer buffer = ByteBuffer.allocate(27);
        buffer.order(ByteOrder.BIG_ENDIAN);

        buffer.putShort((short) 0xf0cb);

        buffer.put((byte) 0x03);
        buffer.put((byte) (elapsedTime / 60));
        buffer.put((byte) (elapsedTime % 60));
        buffer.put((byte) 0xff);

        buffer.put((byte) 0x01);
        buffer.putShort((short) kCal);
        buffer.put((byte) 0x00);

        buffer.put((byte) 0x01);
        buffer.putShort((short) heartRate);
        buffer.put((byte) 0x00);

        buffer.put((byte) 0x01);
        buffer.putShort(((short) (incline * 10)));
        buffer.put((byte) 0x01);

        buffer.put((byte) 0x01);
        buffer.putShort((short) (speed * 10));
        buffer.put((byte) 0x01);

        buffer.put((byte) 0x01);
        buffer.putShort((short) (distance * 10));
        buffer.put((byte) 0x01);

        buffer.put((byte) 0x00);

        byte[] cmdSetDisplayValues = buffer.array();

        cmdSetDisplayValues[cmdSetDisplayValues.length - 1] = getChecksum(cmdSetDisplayValues);

        writeChunked(builder, cmdSetDisplayValues);
    }

    private void setParameters(TransactionBuilder builder, float speed, float incline, boolean btledOn) {
        byte[] cmdSetParameters = COMMAND_SET_PARAMETERS.clone();
        int intSpeed = (int) (speed * 10);
        int intIncline = (int) (incline * 10);
        cmdSetParameters[4] = (byte) (intSpeed >> 8);
        cmdSetParameters[5] = (byte) (intSpeed & 0xff);
        cmdSetParameters[13] = (byte) (intIncline >> 8);
        cmdSetParameters[14] = (byte) (intIncline & 0xff);
        cmdSetParameters[18] = (byte) (btledOn ? 0x01 : 0x00);

        cmdSetParameters[cmdSetParameters.length - 1] = getChecksum(cmdSetParameters);

        writeChunked(builder, cmdSetParameters);
    }

    void writeChunked(TransactionBuilder builder, byte[] data) {
        final int MAX_CHUNKLENGTH = 20;
        int remaining = data.length;
        byte count = 0;
        while (remaining > 0) {
            int copybytes = Math.min(remaining, MAX_CHUNKLENGTH);
            byte[] chunk = new byte[copybytes];

            System.arraycopy(data, count++ * MAX_CHUNKLENGTH, chunk, 0, copybytes);
            builder.write(getCharacteristic(UUUD_CHARACTERISTICS_WRITE), chunk);
            remaining -= copybytes;
        }
        builder.wait(100);
    }

    private byte getChecksum(byte[] command) {
        byte checksum = 0;
        for (byte b : command) {
            checksum += +b;
        }
        return checksum;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }


    @Override
    public boolean useAutoConnect() {
        return false;
    }

    @Override
    public void onNotification(NotificationSpec notificationSpec) {
    }

    @Override
    public void onDeleteNotification(int id) {

    }

    @Override
    public void onSetTime() {

    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {

    }

    @Override
    public void onSetCallState(CallSpec callSpec) {

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
        byte[] command = new byte[]{(byte) 0xf0, (byte) 0xaf, (byte) (start ? 0x01 : 0x00), 0x00};
        command[3] = getChecksum(command);
        BluetoothGattCharacteristic characteristic = getCharacteristic(UUUD_CHARACTERISTICS_WRITE);

        TransactionBuilder builder = new TransactionBuilder("beep");
        builder.write(characteristic, command);
        builder.queue(getQueue());

    }


    @Override
    public void onSetConstantVibration(int intensity) {
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
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();

        if (characteristicUUID.equals(UUUD_CHARACTERISTICS_NOTIFY)) {
            byte[] data = characteristic.getValue();
            if (data.length == 6) { // FIXME: this is assumed the tail of the data below which does not fit inside the MTU
                System.arraycopy(data, 0, last_data, 20, 6);
                ByteBuffer buf = ByteBuffer.wrap(last_data);
                buf.order(ByteOrder.BIG_ENDIAN);
                buf.getShort(); // command
                float incline = buf.getShort() / 10.0f;
                if (incline >= 100) {
                    incline -= 100;
                }
                buf.getShort(); // ??
                float speed = buf.getShort() / 10.0f;
                buf.getShort(); // ??
                int calories = buf.getShort();
                float distance = buf.getShort() / 10.0f;
                buf.getShort(); // ??
                boolean tabletStandUsed = buf.get() > 0;
                int heartRate = buf.getShort();
                float averageSpeed = buf.getShort() / 10.0f;
                boolean keyPluggedIn = buf.get() > 0;
                byte buttonCode = buf.get();
                buf.get(); // ??
                boolean workoutStarted = buf.get() > 0;

                TransactionBuilder builder = new TransactionBuilder("send update");

                if (buttonCode == 6 || buttonCode == 7) {
                    if (workoutStarted || buttonCode == 7) {
                        writeChunked(builder, COMMAND_STOP);
                    } else {
                        writeChunked(builder, COMMAND_START);
                        start_time = (int) (System.currentTimeMillis() / 1000);
                    }
                }
                builder.wait(200);
                writeChunked(builder, COMMAND_REQUEST_DATA);

                int time = (int) (System.currentTimeMillis() / 1000);
                if (last_time != time) {
                    int timeElapsed = time - start_time;
                    setDisplayValues(builder, timeElapsed, calories, heartRate, incline, speed, distance);
                    last_time = time;
                }

                builder.queue(getQueue());

                LOG.debug("speed: " + speed + " incline: " + incline + " distance: " + distance + " calories: " + calories + " average speed: " + averageSpeed + " heart rate: " + heartRate);
                LOG.debug("key plugged in: " + keyPluggedIn + " tablet stand used: " + tabletStandUsed + " buttonCode: " + buttonCode + " workout started: " + workoutStarted);
            } else if (data.length == 20 && data[0] == (byte) 0xf0 && data[1] == (byte) 0xbc) {
                System.arraycopy(data, 0, last_data, 0, 20);
            }
            return true;
        }
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt,
                                        BluetoothGattCharacteristic characteristic, int status) {
        if (super.onCharacteristicRead(gatt, characteristic, status)) {
            return true;
        }
        UUID characteristicUUID = characteristic.getUuid();

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }

    @Override
    public void onSendConfiguration(String config) {

    }

    @Override
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {
        TransactionBuilder builder = new TransactionBuilder("xxx");
        //setDisplayValues(builder, 1, 10, 10, 10, 10);
        //writeChunked(builder, COMMAND_SET_DISPLAY);

        builder.queue(getQueue());
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }
}
