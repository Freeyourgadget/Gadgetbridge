/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miscale;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.devices.miscale.MiScaleSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.MiScaleWeightSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.serial.GBDeviceProtocol;

import static nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst.*;

public class MiSmartScaleDeviceSupport extends AbstractBTLEDeviceSupport {
    private static final Logger LOG = LoggerFactory.getLogger(MiSmartScaleDeviceSupport.class);

    private static final UUID UUID_CHARACTERISTIC_CONFIG = UUID.fromString("00001542-0000-3512-2118-0009af100700");
    private static final UUID UUID_CHARACTERISTIC_WEIGHT_HISTORY = UUID.fromString("00002a2f-0000-3512-2118-0009af100700");

    // There's unfortunately no way to query the config options, they can only be set
    private static final byte CFG_WEIGHT_UNIT = (byte)0x04;
    private static final byte CFG_SMALL_OBJECTS = (byte)0x10;
    private static final byte CFG_RESET_HISTORY = (byte)0x12;

    private static final byte CMD_HISTORY_START = (byte)0x01;
    private static final byte CMD_HISTORY_QUERY = (byte)0x02;
    private static final byte CMD_HISTORY_COMPLETE = (byte)0x03;
    private static final byte CMD_HISTORY_END = (byte)0x04;

    // Threshold for small objects
    private static final int SMALL_OBJECT_MAX_WEIGHT = 10;

    private long userId = -1;

    private final DeviceInfoProfile<MiSmartScaleDeviceSupport> deviceInfoProfile;

    public MiSmartScaleDeviceSupport() {
        super(LOG);

        // Get unique user ID for weight history querying
        try (DBHandler db = GBApplication.acquireDB()) {
            userId = DBHelper.getUser(db.getDaoSession()).getId();
        } catch (Exception e) {
            LOG.error("Error acquiring database", e);
        }

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(intent -> {
            if (!DeviceInfoProfile.ACTION_DEVICE_INFO.equals(intent.getAction()))
                return;

            DeviceInfo info = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);

            if (info == null)
                return;

            GBDeviceEventVersionInfo event = new GBDeviceEventVersionInfo();
            event.fwVersion = info.getSoftwareRevision();
            event.hwVersion = info.getHardwareRevision();

            handleGBDeviceEvent(event);
        });

        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_WEIGHT_SCALE);
        addSupportedService(UUID.fromString(MiBandService.UUID_SERVICE_WEIGHT_SERVICE));
        addSupportedProfile(deviceInfoProfile);
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        deviceInfoProfile.requestDeviceInfo(builder);

        if (GBApplication.getPrefs().getBoolean("datetime_synconconnect", true))
            setTime(builder);

        builder.notify(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT), true);
        builder.notify(getCharacteristic(UUID_CHARACTERISTIC_WEIGHT_HISTORY), true);

        // Query weight measurements saved by the scale
        sendHistoryCommand(builder, CMD_HISTORY_START, true);
        sendHistoryCommand(builder, CMD_HISTORY_QUERY, false);

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));

        return builder;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (super.onCharacteristicChanged(gatt, characteristic))
            return true;

        UUID uuid = characteristic.getUuid();

        if (!uuid.equals(GattCharacteristic.UUID_CHARACTERISTIC_WEIGHT_MEASUREMENT) &&
            !uuid.equals(UUID_CHARACTERISTIC_WEIGHT_HISTORY))
            return false;

        byte[] data = characteristic.getValue();

        if (data.length == 1 && data[0] == CMD_HISTORY_COMPLETE) {
            TransactionBuilder builder = createTransactionBuilder("ack");

            // Acknowledge weight history reception
            sendHistoryCommand(builder, CMD_HISTORY_COMPLETE, false);
            sendHistoryCommand(builder, CMD_HISTORY_END, true);
            builder.notify(getCharacteristic(UUID_CHARACTERISTIC_WEIGHT_HISTORY), false);
            builder.queue(getQueue());
        } else {
            ByteBuffer buf = ByteBuffer.wrap(characteristic.getValue());
            List<WeightMeasurement> measurements = new ArrayList<>();
            WeightMeasurement measurement = WeightMeasurement.decode(buf);

            // Weight history characteristic often has two measurements in one packet
            while (measurement != null) {
                measurements.add(measurement);
                measurement = WeightMeasurement.decode(buf);
            }

            saveMeasurements(measurements);
        }

        return true;
    }

    @Override
    public void onReset(int flags) {
        if ((flags & GBDeviceProtocol.RESET_FLAGS_FACTORY_RESET) == 0)
            return;

        try {
            TransactionBuilder builder = performInitialized("reset");

            setConfigValue(builder, CFG_RESET_HISTORY, (byte)0x00);
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error", e);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());

        try {
            TransactionBuilder builder = performInitialized("config");

            if (config.equals(PREF_MISCALE_WEIGHT_UNIT)) {
                int unit = Integer.parseInt(prefs.getString(PREF_MISCALE_WEIGHT_UNIT, "0"));

                setConfigValue(builder, CFG_WEIGHT_UNIT, (byte)unit);
            } else if (config.equals(PREF_MISCALE_SMALL_OBJECTS)) {
                boolean enabled = prefs.getBoolean(PREF_MISCALE_SMALL_OBJECTS, false);

                setConfigValue(builder, CFG_SMALL_OBJECTS, enabled ? (byte)0x01: (byte)0x00);
            }

            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.error("Error", e);
        }
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    private void setTime(TransactionBuilder builder) {
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] time = BLETypeConversions.calendarToCurrentTime(now, 0);

        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), time);
    }

    private void setConfigValue(TransactionBuilder builder, byte config, byte value) {
        byte[] data = new byte[] { (byte)0x06, config, (byte)0x00, value };

        builder.write(getCharacteristic(UUID_CHARACTERISTIC_CONFIG), data);
    }

    private void sendHistoryCommand(TransactionBuilder builder, byte cmd, boolean includeUserId) {
        ByteBuffer buf = ByteBuffer.allocate(includeUserId ? 5 : 1);

        buf.put(cmd);

        // The user ID is directly related to the account ID in the Zepp Life app
        if (includeUserId)
            buf.putInt((int)userId);

        builder.write(getCharacteristic(UUID_CHARACTERISTIC_WEIGHT_HISTORY), buf.array());
    }

    private void saveMeasurements(List<WeightMeasurement> measurements) {
        SharedPreferences prefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        boolean allowSmallObjects = prefs.getBoolean(PREF_MISCALE_SMALL_OBJECTS, false);

        try (DBHandler db = GBApplication.acquireDB()) {
            MiScaleSampleProvider provider = new MiScaleSampleProvider(getDevice(), db.getDaoSession());
            List<MiScaleWeightSample> samples = new ArrayList<>();
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();

            for (WeightMeasurement measurement : measurements) {
                // Skip measurements of small objects if not allowed
                if (!allowSmallObjects && measurement.getWeightKg() < SMALL_OBJECT_MAX_WEIGHT)
                    continue;

                samples.add(new MiScaleWeightSample(
                    measurement.getTimestamp().getTime(),
                    deviceId,
                    userId,
                    measurement.getWeightKg()
                ));
            }

            provider.addSamples(samples);
        } catch (Exception e) {
            LOG.error("Error acquiring database", e);
        }
    }
}
