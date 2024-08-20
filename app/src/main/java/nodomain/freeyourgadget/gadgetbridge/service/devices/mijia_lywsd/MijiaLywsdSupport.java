/*  Copyright (C) 2023-2024 José Rebelo, Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.mijia_lywsd;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.SimpleTimeZone;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.AbstractMijiaLywsdCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;
import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class MijiaLywsdSupport extends AbstractBTLEDeviceSupport {

    private static final Logger LOG = LoggerFactory.getLogger(MijiaLywsdSupport.class);
    private static final UUID UUID_TIME = UUID.fromString("ebe0ccb7-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_BATTERY = UUID.fromString("ebe0ccc4-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_SCALE = UUID.fromString("ebe0ccbe-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_COMFORT_LEVEL = UUID.fromString("ebe0ccd7-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_CONN_INTERVAL = UUID.fromString("ebe0ccd8-7a0a-4b0c-8a1a-6ff2997da3a6");
    private final DeviceInfoProfile<MijiaLywsdSupport> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = new IntentListener() {
        @Override
        public void notify(Intent intent) {
            String s = intent.getAction();
            if (Objects.equals(s, DeviceInfoProfile.ACTION_DEVICE_INFO)) {
                handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
            }
        }
    };

    // Length of comfort level characteristic for different devices
    public static final int COMFORT_LEVEL_LENGTH_LYWSD03 = 6;
    public static final int COMFORT_LEVEL_LENGTH_XMWSDJ04 = 8;

    public MijiaLywsdSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(UUID.fromString("ebe0ccb0-7a0a-4b0c-8a1a-6ff2997da3a6"));
        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(mListener);
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));
        requestDeviceInfo(builder);

        final boolean supportsSetTime = getCoordinator().supportsSetTime();
        if (supportsSetTime && GBApplication.getPrefs().getBoolean("datetime_synconconnect", true)) {
            setTime(builder);
        }

        getBatteryInfo(builder);
        getComfortLevel(builder);
        setConnectionInterval(builder);
        setInitialized(builder);
        return builder;
    }

    protected AbstractMijiaLywsdCoordinator getCoordinator() {
        return (AbstractMijiaLywsdCoordinator) gbDevice.getDeviceCoordinator();
    }

    private void setTime(TransactionBuilder builder) {
        BluetoothGattCharacteristic timeCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_TIME);
        long ts = System.currentTimeMillis();
        byte offsetHours = (byte) (SimpleTimeZone.getDefault().getOffset(ts) / (1000 * 60 * 60));
        ts = (ts + 250 + 500) / 1000; // round to seconds with +250 ms to compensate for BLE connection interval
        builder.write(timeCharacteristc, new byte[]{
                (byte) (ts & 0xff),
                (byte) ((ts >> 8) & 0xff),
                (byte) ((ts >> 16) & 0xff),
                (byte) ((ts >> 24) & 0xff),
                offsetHours});
    }

    private void setConnectionInterval(TransactionBuilder builder) {
        BluetoothGattCharacteristic intervalCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_CONN_INTERVAL);
        builder.write(intervalCharacteristc, new byte[]{(byte) 0xf4, (byte) 0x01}); // maximum interval of 500 ms
    }

    private void getBatteryInfo(TransactionBuilder builder) {
        BluetoothGattCharacteristic batteryCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_BATTERY);
        builder.read(batteryCharacteristc);
    }

    private void getComfortLevel(TransactionBuilder builder) {
        BluetoothGattCharacteristic comfortCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_COMFORT_LEVEL);
        builder.read(comfortCharacteristc);
    }

    private void setTemperatureScale(TransactionBuilder builder, SharedPreferences prefs) {
        String scale = prefs.getString(PREF_TEMPERATURE_SCALE_CF, "");
        BluetoothGattCharacteristic scaleCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_SCALE);
        builder.write(scaleCharacteristc, new byte[]{(byte) ("f".equals(scale) ? 0x01 : 0xff)});
    }

    private void setComfortLevel(TransactionBuilder builder, SharedPreferences prefs) {
        int length = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_CHARACTERISTIC_LENGTH, 0);
        int temperatureLower = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER, 19);
        int temperatureUpper = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER, 27);
        int humidityLower = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER, 20);
        int humidityUpper = prefs.getInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER, 85);

        // Ignore invalid values
        if (temperatureLower > temperatureUpper || humidityLower > humidityUpper)
            return;

        BluetoothGattCharacteristic comfortCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_COMFORT_LEVEL);
        ByteBuffer buf = ByteBuffer.allocate(length);

        buf.order(ByteOrder.LITTLE_ENDIAN);

        switch (length) {
            case COMFORT_LEVEL_LENGTH_LYWSD03:
                buf.putShort((short)(temperatureUpper * 100));
                buf.putShort((short)(temperatureLower * 100));
                buf.put((byte)humidityUpper);
                buf.put((byte)humidityLower);
                break;
            case COMFORT_LEVEL_LENGTH_XMWSDJ04:
                buf.putShort((short)(temperatureUpper * 10));
                buf.putShort((short)(temperatureLower * 10));
                buf.putShort((short)(humidityUpper * 10));
                buf.putShort((short)(humidityLower * 10));
                break;
            default:
                return;
        }

        builder.write(comfortCharacteristc, buf.array());
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            batteryCmd.level = ((short) value[0]);
            batteryCmd.state = (batteryCmd.level > 20) ? BatteryState.BATTERY_NORMAL : BatteryState.BATTERY_LOW;
            handleGBDeviceEvent(batteryCmd);
        }
    }

    private void handleComfortLevel(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS)
            return;

        ByteBuffer buf = ByteBuffer.wrap(value);
        int temperatureLower, temperatureUpper;
        int humidityLower, humidityUpper;

        buf.order(ByteOrder.LITTLE_ENDIAN);

        switch (value.length) {
            case COMFORT_LEVEL_LENGTH_LYWSD03:
                temperatureUpper = buf.getShort() / 100;
                temperatureLower = buf.getShort() / 100;
                humidityUpper = buf.get();
                humidityLower = buf.get();
                break;
            case COMFORT_LEVEL_LENGTH_XMWSDJ04:
                temperatureUpper = buf.getShort() / 10;
                temperatureLower = buf.getShort() / 10;
                humidityUpper = buf.getShort() / 10;
                humidityLower = buf.getShort() / 10;
                break;
            default:
                LOG.error("Unknown comfort level characteristic: {}", hexdump(value));
                return;
        }

        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        prefs.edit()
             .putInt(PREF_MIJIA_LYWSD_COMFORT_CHARACTERISTIC_LENGTH, value.length)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER, temperatureLower)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER, temperatureUpper)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER, humidityLower)
             .putInt(PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER, humidityUpper)
             .apply();
    }

    private void requestDeviceInfo(TransactionBuilder builder) {
        LOG.debug("Requesting Device Info!");
        deviceInfoProfile.requestDeviceInfo(builder);
    }

    private void setInitialized(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
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

    @Override
    public void onSetTime() {
        TransactionBuilder builder;
        try {
            builder = performInitialized("Set time");
            setTime(builder);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error setting time on LYWSD02", e);
            GB.toast("Error setting time on LYWSD02", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt,
                                           BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic)) {
            return true;
        }

        UUID characteristicUUID = characteristic.getUuid();
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

        if (MijiaLywsdSupport.UUID_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), status);
            return true;
        }

        if (MijiaLywsdSupport.UUID_COMFORT_LEVEL.equals(characteristicUUID)) {
            handleComfortLevel(characteristic.getValue(), status);
            return true;
        }

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }

    @Override
    public void onSendConfiguration(String config) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress());

        try {
            TransactionBuilder builder = performInitialized("Sending configuration for option: " + config);

            switch (config) {
                case PREF_TEMPERATURE_SCALE_CF:
                    setTemperatureScale(builder, prefs);
                    break;
                case PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_LOWER:
                case PREF_MIJIA_LYWSD_COMFORT_TEMPERATURE_UPPER:
                case PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_LOWER:
                case PREF_MIJIA_LYWSD_COMFORT_HUMIDITY_UPPER:
                    setComfortLevel(builder, prefs);
                    break;
            }

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error setting configuration on LYWSD02", e);
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    @Override
    public boolean getImplicitCallbackModify() {
        return true;
    }

    @Override
    public boolean getSendWriteRequestResponse() {
        return false;
    }
}
