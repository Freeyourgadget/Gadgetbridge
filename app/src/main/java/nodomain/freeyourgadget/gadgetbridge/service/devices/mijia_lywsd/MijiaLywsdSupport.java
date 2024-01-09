/*  Copyright (C) 2016-2021 Andreas Shimokawa, Carsten Pfeiffer, Sebastian
    Kranz, Davis Mosenkovs, José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.mijia_lywsd;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.AbstractMijiaLywsdCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaLywsdHistoricSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.mijia_lywsd.MijiaLywsdRealtimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdHistoricSample;
import nodomain.freeyourgadget.gadgetbridge.entities.MijiaLywsdRealtimeSample;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class MijiaLywsdSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MijiaLywsdSupport.class);

    private static final UUID UUID_BASE_SERVICE = UUID.fromString("ebe0ccb0-7a0a-4b0c-8a1a-6ff2997da3a6");

    private static final UUID UUID_TIME = UUID.fromString("ebe0ccb7-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_BATTERY = UUID.fromString("ebe0ccc4-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_SCALE = UUID.fromString("ebe0ccbe-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_CONN_INTERVAL = UUID.fromString("ebe0ccd8-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_HISTORY = UUID.fromString("ebe0ccbc-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_LIVE_DATA = UUID.fromString("ebe0ccc1-7a0a-4b0c-8a1a-6ff2997da3a6");
    private static final UUID UUID_HISTORY_LAST_ID = UUID.fromString("ebe0ccba-7a0a-4b0c-8a1a-6ff2997da3a6");

    private final DeviceInfoProfile<MijiaLywsdSupport> deviceInfoProfile;
    private final GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
    private final GBDeviceEventBatteryInfo batteryCmd = new GBDeviceEventBatteryInfo();
    private final IntentListener mListener = intent -> {
        String s = intent.getAction();
        if (Objects.equals(s, DeviceInfoProfile.ACTION_DEVICE_INFO)) {
            handleDeviceInfo((nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo) intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO));
        }
    };

    private int startupTime = 0;

    public MijiaLywsdSupport() {
        super(LOG);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(MijiaLywsdSupport.UUID_BASE_SERVICE);

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
        } else {
            getTime(builder);
        }

        // TODO: We can't enable this without properly handling the historic data id and live data, otherwise
        // it will cause battery drain on both the phone and device
        //builder.notify(getCharacteristic(MijiaLywsdSupport.UUID_HISTORY), true);
        //builder.notify(getCharacteristic(MijiaLywsdSupport.UUID_LIVE_DATA), true);

        getBatteryInfo(builder);
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

    private void getTime(TransactionBuilder builder) {
        BluetoothGattCharacteristic timeCharacteristic = getCharacteristic(MijiaLywsdSupport.UUID_TIME);
        builder.read(timeCharacteristic);
    }

    private void setTemperatureScale(TransactionBuilder builder, String scale) {
        BluetoothGattCharacteristic scaleCharacteristc = getCharacteristic(MijiaLywsdSupport.UUID_SCALE);
        builder.write(scaleCharacteristc, new byte[]{(byte) ("f".equals(scale) ? 0x01 : 0xff)});
    }

    private void handleBatteryInfo(byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleBatteryInfo: {}", status);
            return;
        }

        batteryCmd.level = ((short) value[0]);
        batteryCmd.state = (batteryCmd.level > 20) ? BatteryState.BATTERY_NORMAL : BatteryState.BATTERY_LOW;
        handleGBDeviceEvent(batteryCmd);
    }

    private void handleHistory(final byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleHistory: {}", status);
            return;
        }

        if (value.length != 14) {
            LOG.warn("Unexpected history length {}", value.length);
            return;
        }

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        final int id = buf.getInt();
        final int uptimeOffset = buf.getInt();
        final int maxTemperature = buf.getShort();
        final int maxHumidity = buf.get() & 0xff;
        final int minTemperature = buf.getShort();
        final int minHumidity = buf.get() & 0xff;

        // Devices that do not support setting the time report the live data as an offset from the uptime
        // other devices report the correct timestamp.
        final int ts = (!getCoordinator().supportsSetTime() ? startupTime : 0) + uptimeOffset;

        LOG.info(
                "Got history: id={}, uptimeOffset={}, ts={}, minTemperature={}, maxTemperature={}, minHumidity={}, maxHumidity={}",
                id,
                uptimeOffset,
                ts,
                minTemperature / 10.0f,
                maxTemperature / 10.0f,
                minHumidity,
                maxHumidity
        );

        if (!getCoordinator().supportsSetTime() && startupTime <= 0) {
            LOG.warn("Startup time is unknown - ignoring sample");
            return;
        }

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final MijiaLywsdHistoricSampleProvider sampleProvider = new MijiaLywsdHistoricSampleProvider(gbDevice, session);

            final MijiaLywsdHistoricSample sample = sampleProvider.createSample();
            sample.setTimestamp(ts * 1000L);
            sample.setMinTemperature(minTemperature / 10.0f);
            sample.setMaxTemperature(maxTemperature / 10.0f);
            sample.setMinHumidity(minHumidity);
            sample.setMaxHumidity(maxHumidity);
            sample.setDevice(device);
            sample.setUser(user);

            sampleProvider.addSample(sample);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving historic sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving historic samples", e);
        }
    }

    private void handleLiveData(final byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleLiveData: {}", status);
            return;
        }

        if (value.length != 5) {
            LOG.warn("Unexpected live data length {}", value.length);
            return;
        }

        final ByteBuffer buf = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);

        final int temperature = buf.getShort();
        final int humidity = buf.get() & 0xff;
        final int voltage = buf.getShort();

        LOG.info(
                "Got mijia live data: temperature={}, humidity={}, voltage={}",
                temperature / 100f,
                humidity,
                voltage / 1000f
        );

        try (DBHandler handler = GBApplication.acquireDB()) {
            final DaoSession session = handler.getDaoSession();
            final GBDevice gbDevice = getDevice();
            final Device device = DBHelper.getDevice(gbDevice, session);
            final User user = DBHelper.getUser(session);

            final MijiaLywsdRealtimeSampleProvider sampleProvider = new MijiaLywsdRealtimeSampleProvider(gbDevice, session);

            final MijiaLywsdRealtimeSample sample = sampleProvider.createSample();
            sample.setTimestamp(System.currentTimeMillis());
            sample.setTemperature(temperature / 100.0f);
            sample.setHumidity(humidity);
            sample.setDevice(device);
            sample.setUser(user);

            sampleProvider.addSample(sample);
        } catch (final Exception e) {
            GB.toast(getContext(), "Error saving historic sample", Toast.LENGTH_LONG, GB.ERROR);
            LOG.error("Error saving historic samples", e);
        }

        // Warning: this voltage value is not reliable, so I am not sure
        // it's even worth usiong
        //batteryCmd.voltage = voltage / 1000f;
        //handleGBDeviceEvent(batteryCmd);
    }

    private void handleTime(final byte[] value, int status) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            LOG.warn("Unsuccessful response for handleTime: {}", status);
            return;
        }

        if (value.length != 4) {
            LOG.warn("Unexpected time length {}", value.length);
            return;
        }

        final int uptime = BLETypeConversions.toUint32(value);

        startupTime = (int) ((System.currentTimeMillis() / 1000L) - uptime);

        LOG.info("Got mijia time={}, startupTime={}", uptime, startupTime);
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
        LOG.info("Device info: {}", info);
        versionCmd.hwVersion = info.getHardwareRevision();
        versionCmd.fwVersion = info.getFirmwareRevision();
        handleGBDeviceEvent(versionCmd);
    }

    @Override
    public void onSetTime() {
        if (!getCoordinator().supportsSetTime()) {
            LOG.warn("setting time is not supported by this device");
            return;
        }

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

        if (MijiaLywsdSupport.UUID_HISTORY.equals(characteristicUUID)) {
            handleHistory(characteristic.getValue(), BluetoothGatt.GATT_SUCCESS);
            return true;
        } else if (MijiaLywsdSupport.UUID_LIVE_DATA.equals(characteristicUUID)) {
            handleLiveData(characteristic.getValue(), BluetoothGatt.GATT_SUCCESS);
            return true;
        } else if (MijiaLywsdSupport.UUID_TIME.equals(characteristicUUID)) {
            handleTime(characteristic.getValue(), BluetoothGatt.GATT_SUCCESS);
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

        if (MijiaLywsdSupport.UUID_BATTERY.equals(characteristicUUID)) {
            handleBatteryInfo(characteristic.getValue(), status);
            return true;
        } else if (MijiaLywsdSupport.UUID_HISTORY.equals(characteristicUUID)) {
            handleHistory(characteristic.getValue(), status);
            return true;
        } else if (MijiaLywsdSupport.UUID_LIVE_DATA.equals(characteristicUUID)) {
            handleLiveData(characteristic.getValue(), status);
            return true;
        } else if (MijiaLywsdSupport.UUID_TIME.equals(characteristicUUID)) {
            handleTime(characteristic.getValue(), status);
            return true;
        }

        LOG.info("Unhandled characteristic read: " + characteristicUUID);
        return false;
    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        try {
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_TEMPERATURE_SCALE_CF:
                    String temperatureScale = GBApplication.getDeviceSpecificSharedPrefs(gbDevice.getAddress()).getString(DeviceSettingsPreferenceConst.PREF_TEMPERATURE_SCALE_CF, "");
                    builder = performInitialized("Sending configuration for option: " + config);
                    setTemperatureScale(builder, temperatureScale);
                    builder.queue(getQueue());
                    break;
            }
        } catch (IOException e) {
            LOG.error("Error setting configuration on LYWSD02", e);
            GB.toast("Error setting configuration", Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }
}
