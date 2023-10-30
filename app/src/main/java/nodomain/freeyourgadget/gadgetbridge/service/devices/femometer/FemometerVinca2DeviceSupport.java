/*  Copyright (C) 2023 Alicia Hormann

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package nodomain.freeyourgadget.gadgetbridge.service.devices.femometer;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.femometer.FemometerVinca2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.FemometerVinca2TemperatureSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.IntentListener;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfo;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.deviceinfo.DeviceInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.healthThermometer.HealthThermometerProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.healthThermometer.TemperatureInfo;

public class FemometerVinca2DeviceSupport extends AbstractBTLEDeviceSupport {

    private final DeviceInfoProfile<FemometerVinca2DeviceSupport> deviceInfoProfile;
    private final BatteryInfoProfile<FemometerVinca2DeviceSupport> batteryInfoProfile;
    private final HealthThermometerProfile<FemometerVinca2DeviceSupport> healthThermometerProfile;
    private static final Logger LOG = LoggerFactory.getLogger(FemometerVinca2DeviceSupport.class);

    public static final UUID UNKNOWN_SERVICE_UUID = UUID.fromString((String.format(AbstractBTLEDeviceSupport.BASE_UUID, "fef5")));
    // Characteristic 8082caa8-41a6-4021-91c6-56f9b954cc34 READ WRITE
    // Characteristic 9d84b9a3-000c-49d8-9183-855b673fda31 READ WRITE
    // Characteristic 457871e8-d516-4ca1-9116-57d0b17b9cb2 READ WRITE NO RESPONSE WRITE
    // Characteristic 5f78df94-798c-46f5-990a-b3eb6a065c88 READ NOTIFY

    public static final UUID CONFIGURATION_SERVICE_UUID = UUID.fromString("0f0e0d0c-0b0a-0908-0706-050403020100");
    public static final UUID CONFIGURATION_SERVICE_ALARM_CHARACTERISTIC = UUID.fromString("1f1e1d1c-1b1a-1918-1716-151413121110"); // READ WRITE
    public static final UUID CONFIGURATION_SERVICE_SETTING_CHARACTERISTIC = UUID.fromString("2f2e2d2c-2b2a-2928-2726-252423222120"); // WRITE
    public static final UUID CONFIGURATION_SERVICE_INDICATION_CHARACTERISTIC = UUID.fromString("3f3e3d3c-3b3a-3938-3736-353433323130"); // INDICATE

    public FemometerVinca2DeviceSupport() {
        super(LOG);

        /// Initialize Services
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ACCESS);
        addSupportedService(GattService.UUID_SERVICE_GENERIC_ATTRIBUTE);
        addSupportedService(GattService.UUID_SERVICE_BATTERY_SERVICE);
        addSupportedService(GattService.UUID_SERVICE_DEVICE_INFORMATION);
        addSupportedService(GattService.UUID_SERVICE_HEALTH_THERMOMETER);
        addSupportedService(GattService.UUID_SERVICE_CURRENT_TIME);
        addSupportedService(GattService.UUID_SERVICE_REFERENCE_TIME_UPDATE);
        addSupportedService(UNKNOWN_SERVICE_UUID);
        addSupportedService(CONFIGURATION_SERVICE_UUID);

        /// Device Info
        IntentListener deviceInfoListener = intent -> {
            String action = intent.getAction();
            if (DeviceInfoProfile.ACTION_DEVICE_INFO.equals(action)) {
                DeviceInfo info = intent.getParcelableExtra(DeviceInfoProfile.EXTRA_DEVICE_INFO);
                if (info == null) return;
                GBDeviceEventVersionInfo versionCmd = new GBDeviceEventVersionInfo();
                versionCmd.hwVersion = info.getHardwareRevision();
                versionCmd.fwVersion = info.getSoftwareRevision(); // firmwareRevision always reported as null
                handleGBDeviceEvent(versionCmd);
            }
        };

        deviceInfoProfile = new DeviceInfoProfile<>(this);
        deviceInfoProfile.addListener(deviceInfoListener);
        addSupportedProfile(deviceInfoProfile);

        /// Battery
        IntentListener batteryListener = intent -> {
            BatteryInfo info = intent.getParcelableExtra(BatteryInfoProfile.EXTRA_BATTERY_INFO);
            if (info == null) return;
            GBDeviceEventBatteryInfo batteryEvent = new GBDeviceEventBatteryInfo();
            batteryEvent.state = BatteryState.BATTERY_NORMAL;
            batteryEvent.level = info.getPercentCharged();
            evaluateGBDeviceEvent(batteryEvent);
            handleGBDeviceEvent(batteryEvent);
        };
        batteryInfoProfile = new BatteryInfoProfile<>(this);
        batteryInfoProfile.addListener(batteryListener);
        addSupportedProfile(batteryInfoProfile);


        /// Temperature
        IntentListener temperatureListener = intent -> {
            TemperatureInfo info = intent.getParcelableExtra(HealthThermometerProfile.EXTRA_TEMPERATURE_INFO);
            if (info == null) return;
            handleMeasurement(info);
        };
        healthThermometerProfile = new HealthThermometerProfile<>(this);
        healthThermometerProfile.addListener(temperatureListener);
        addSupportedProfile(healthThermometerProfile);
    }

    @Override
    public boolean useAutoConnect() {
        return false;
    }

    /**
     * @param data An int smaller equal 255 (0xff)
     */
    private byte[] byteArray(int data) {
        return new byte[]{(byte) data};
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        // Init Battery
        batteryInfoProfile.requestBatteryInfo(builder);
        batteryInfoProfile.enableNotify(builder, true);

        // Init Device Info
        getDevice().setFirmwareVersion("N/A");
        getDevice().setFirmwareVersion2("N/A");
        deviceInfoProfile.requestDeviceInfo(builder);

        // Mystery stuff that happens in original app, not sure if its required
        BluetoothGattCharacteristic c2 = getCharacteristic(CONFIGURATION_SERVICE_SETTING_CHARACTERISTIC);
        builder.write(c2, byteArray(0x21));
        builder.write(c2, byteArray(0x02));
        builder.write(c2, byteArray(0x03));
        builder.write(c2, byteArray(0x05));

        // Sync Time
        setCurrentTime(builder);

        // Init Thermometer
        builder.notify(getCharacteristic(CONFIGURATION_SERVICE_INDICATION_CHARACTERISTIC), true);
        healthThermometerProfile.enableNotify(builder, true);
        healthThermometerProfile.setMeasurementInterval(builder, new byte[]{(byte) 0x01, (byte) 0x00});

        // mark the device as initialized
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        return builder;
    }

    @Override
    public void onSetTime() {
        TransactionBuilder builder = new TransactionBuilder("set time");
        setCurrentTime(builder);
        builder.queue(getQueue());
    }

    private void setCurrentTime(TransactionBuilder builder) {
        // Same Code as in PineTime (without the local time)
        GregorianCalendar now = BLETypeConversions.createCalendar();
        byte[] bytesCurrentTime = BLETypeConversions.calendarToCurrentTime(now, 0);
        builder.write(getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_CURRENT_TIME), bytesCurrentTime);
    }

    @Override
    public void onSetAlarms(ArrayList<? extends Alarm> alarms) {
        try {
            TransactionBuilder builder = performInitialized("applyThermometerSetting");

            Alarm alarm  = alarms.get(0);
            byte[] alarm_bytes = new byte[] {
                    (byte) (alarm.getEnabled()? 0x01 : 0x00),  // first byte 01/00: turn alarm on/off
                    (byte) alarm.getHour(),                    // second byte: hour
                    (byte) alarm.getMinute()                   // third byte: minute
            };

            builder.write(getCharacteristic(CONFIGURATION_SERVICE_ALARM_CHARACTERISTIC), alarm_bytes);
            builder.write(getCharacteristic(CONFIGURATION_SERVICE_SETTING_CHARACTERISTIC), byteArray(0x01));
            // read-request on char1 results in given alarm
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn(" Unable to apply setting ", e);
        }
    }

    @Override
    public void onSendConfiguration(String config) {
        TransactionBuilder builder;
        SharedPreferences sharedPreferences = GBApplication.getDeviceSpecificSharedPrefs(this.getDevice().getAddress());
        LOG.info(" onSendConfiguration: " + config);
        try {
            builder = performInitialized("sendConfig: " + config);
            switch (config) {
                case DeviceSettingsPreferenceConst.PREF_FEMOMETER_MEASUREMENT_MODE:
                    setMeasurementMode(sharedPreferences);
                    break;
                case DeviceSettingsPreferenceConst.PREF_VOLUME:
                    setVolume(sharedPreferences);
                    break;
                case DeviceSettingsPreferenceConst.PREF_TEMPERATURE_SCALE_CF:
                    String scale = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_TEMPERATURE_SCALE_CF,  "c");
                    int value = "c".equals(scale) ? 0x0a : 0x0b;
                    applySetting(byteArray(value), null);
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Set Measurement Mode
     * modes (0- quick, 1- normal, 2- long)
     */
    private void setMeasurementMode(SharedPreferences sharedPreferences) {
        String measurementMode = sharedPreferences.getString(DeviceSettingsPreferenceConst.PREF_FEMOMETER_MEASUREMENT_MODE, "normal");
        byte[] confirmation = byteArray(0x1e);
        switch (measurementMode) {
            case "quick":
                applySetting(byteArray(0x1a), confirmation);
                break;
            case "normal":
                applySetting(byteArray(0x1c), confirmation);
                break;
            case "precise":
                applySetting(byteArray(0x1d), confirmation);
                break;
        }
    }

    /** Set Volume
     * volumes 0-30 (0-10: quiet, 11-20: normal, 21-30: loud)
     */
    private void setVolume(SharedPreferences sharedPreferences) {
        int volume = sharedPreferences.getInt(DeviceSettingsPreferenceConst.PREF_VOLUME, 50);
        byte[] confirmation = byteArray(0xfd);
        if (volume < 11) {
            applySetting(byteArray(0x09), confirmation);
        } else if (volume < 21) {
            applySetting(byteArray(0x14), confirmation);
        } else {
            applySetting(byteArray(0x16), confirmation);
        }
    }

    private void applySetting(byte[] value, byte[] confirmation) {
        try {
            TransactionBuilder builder = performInitialized("applyThermometerSetting");
            builder.write(getCharacteristic(CONFIGURATION_SERVICE_SETTING_CHARACTERISTIC), value);
            if (confirmation != null) {
                builder.write(getCharacteristic(CONFIGURATION_SERVICE_SETTING_CHARACTERISTIC), confirmation);
            }
            builder.queue(getQueue());
        } catch (IOException e) {
            LOG.warn(" Unable to apply setting ", e);
        }
    }

    private void handleMeasurement(TemperatureInfo info) {
        Date timestamp = info.getTimestamp();
        float temperature = info.getTemperature();
        int temperatureType = info.getTemperatureType();
        try (DBHandler db = GBApplication.acquireDB()) {
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), db.getDaoSession()).getId();
            long time = timestamp.getTime();

            FemometerVinca2SampleProvider sampleProvider = new FemometerVinca2SampleProvider(getDevice(), db.getDaoSession());
            FemometerVinca2TemperatureSample temperatureSample = new FemometerVinca2TemperatureSample(time, deviceId, userId, temperature, temperatureType);
            sampleProvider.addSample(temperatureSample);
        } catch (Exception e) {
            LOG.error("Error acquiring database", e);
        }
    }
}
