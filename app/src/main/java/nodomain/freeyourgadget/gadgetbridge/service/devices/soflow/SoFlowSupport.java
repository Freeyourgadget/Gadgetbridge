/*  Copyright (C) 2022 Andreas Shimokawa

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.soflow;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
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
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.CryptoUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class SoFlowSupport extends AbstractBTLEDeviceSupport {

    public static final UUID UUID_CHARACTERISICS_NOTIFICATION = UUID.fromString("60000002-0000-1000-8000-00805f9b34fb");
    public static final UUID UUID_CHARACTERISICS_WRITE = UUID.fromString("60000003-0000-1000-8000-00805f9b34fb");
    public static final byte[] COMMAND_REQUEST_SESSION = new byte[]{0x06, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_REQUEST_LOCK = new byte[]{0x05, 0x0e, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public static final byte[] COMMAND_LOCK = new byte[]{0x05, 0x0c, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_UNLOCK = new byte[]{0x05, 0x01, 0x06, 0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    public static final byte[] COMMAND_SET_LIGHT = new byte[]{0x05, 0x47, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    private static final Logger LOG = LoggerFactory.getLogger(SoFlowSupport.class);
    private final DeviceInfoProfile<SoFlowSupport> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();

    private byte[] aesKey;
    private final byte[] session = new byte[]{0, 0, 0, 0};

    public SoFlowSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(UUID.fromString("60000001-0000-1000-8000-00805f9b34fb"));

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        IntentListener mListener = intent -> {
            String s = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(s)) {
                handleDeviceInfo(intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        };
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        aesKey = getSecretKey();
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        requestDeviceInfo(builder);
        builder.notify(getCharacteristic(UUID_CHARACTERISICS_NOTIFICATION), true);
        writeEncrypted(builder, COMMAND_REQUEST_SESSION);
        setInitialized(builder);
        return builder;
    }

    private void requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
    }

    private void writeEncrypted(TransactionBuilder builder, byte[] data) {
        try {
            LOG.debug("will encrypt " + GB.hexdump(data));
            builder.write(getCharacteristic(UUID_CHARACTERISICS_WRITE), CryptoUtils.encryptAES(data, aesKey));
        } catch (Exception e) {
            LOG.error("error while encrypting data");
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void handleDeviceInfo(nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo info) {
        LOG.warn("Device info: " + info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    protected byte[] getSecretKey() {
        byte[] authKeyBytes;

        SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        String authKey = sharedPrefs.getString("authkey", null);
        if (authKey != null && !authKey.isEmpty()) {
            authKey = authKey.trim();
            if (authKey.length() == 34 && authKey.startsWith("0x")) {
                authKeyBytes = GB.hexStringToByteArray(authKey.substring(2));
            } else if (authKey.length() == 32) {
                authKeyBytes = GB.hexStringToByteArray(authKey);
            } else {
                return null;
            }
            return authKeyBytes;
        }
        return null;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
        if (UUID_CHARACTERISICS_NOTIFICATION.equals(characteristicUUID)) {
            try {
                byte[] data = CryptoUtils.decryptAES(characteristic.getValue(), aesKey);
                if (data[0] == 0x06 && data[1] == 0x01 && data[2] == 0x07) {
                    session[0] = data[3];
                    session[1] = data[4];
                    session[2] = data[5];
                    session[3] = data[6];
                    LOG.info("Got session");
                    COMMAND_LOCK[4] = session[0];
                    COMMAND_LOCK[5] = session[1];
                    COMMAND_LOCK[6] = session[2];
                    COMMAND_LOCK[7] = session[3];
                    COMMAND_REQUEST_LOCK[4] = session[0];
                    COMMAND_REQUEST_LOCK[5] = session[1];
                    COMMAND_REQUEST_LOCK[6] = session[2];
                    COMMAND_REQUEST_LOCK[7] = session[3];
                    COMMAND_SET_LIGHT[4] = session[0];
                    COMMAND_SET_LIGHT[5] = session[1];
                    COMMAND_SET_LIGHT[6] = session[2];
                    COMMAND_SET_LIGHT[7] = session[3];
                    COMMAND_UNLOCK[9] = session[0];
                    COMMAND_UNLOCK[10] = session[1];
                    COMMAND_UNLOCK[11] = session[2];
                    COMMAND_UNLOCK[12] = session[3];
                }
                else {
                    GB.toast(GB.hexdump(data),5,1);
                }
            } catch (Exception e) {
                LOG.error("error while decrypting");
            }

            return true;
        }
        LOG.info("Unhandled characteristic changed: " + characteristicUUID);
        return false;
    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        try {
            builder = performInitialized("Sending configuration for option: " + config);
            if ("lock".equals(config)) {
                SharedPreferences sharedPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
                boolean lock = sharedPrefs.getBoolean("lock", false);
                if (lock) {
                    writeEncrypted(builder, COMMAND_LOCK);
                } else {
                    writeEncrypted(builder, COMMAND_UNLOCK);
                }
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
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
    public void onReadConfiguration(String config) {

    }

    @Override
    public void onTestNewFunction() {
        TransactionBuilder builder;
        try {
            builder = performInitialized("request unknown");
            writeEncrypted(builder,COMMAND_REQUEST_LOCK);
            builder.queue(getQueue());
        } catch (IOException e) {
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public void onSendWeather(WeatherSpec weatherSpec) {

    }
}
