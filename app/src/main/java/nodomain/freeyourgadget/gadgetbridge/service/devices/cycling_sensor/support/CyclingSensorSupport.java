package nodomain.freeyourgadget.gadgetbridge.service.devices.cycling_sensor.support;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.cycling_sensor.db.CyclingSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.CyclingSample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.ReadAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.battery.BatteryInfoProfile;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class CyclingSensorSupport extends CyclingSensorBaseSupport {
    static class CyclingSpeedCadenceMeasurement {
        private static final int FLAG_REVOLUTION_DATA_PRESENT = 1 << 0;
        private static final int FLAG_CADENCE_DATA_PRESENT    = 1 << 1;
        public boolean revolutionDataPresent = false;
        public boolean cadenceDataPresent = false;
        public int revolutionCount;
        public int lastRevolutionTimeTicks;
        private int crankRevolutionCount;
        private int lastCrankRevolutionTimeTicks;

        public static CyclingSpeedCadenceMeasurement fromPayload(byte[] payload) throws RuntimeException {
            if(payload.length < 7){
                throw new RuntimeException("wrong payload length");
            }

            ByteBuffer buffer = ByteBuffer
                    .wrap(payload)
                    .order(ByteOrder.LITTLE_ENDIAN);

            byte flags = buffer.get();

            boolean revolutionDataPresent = (flags | FLAG_REVOLUTION_DATA_PRESENT) == FLAG_REVOLUTION_DATA_PRESENT;
            boolean cadenceDataPresent = (flags | FLAG_CADENCE_DATA_PRESENT) == FLAG_CADENCE_DATA_PRESENT;
            CyclingSpeedCadenceMeasurement result = new CyclingSpeedCadenceMeasurement();

            if(revolutionDataPresent){
                result.revolutionDataPresent = true;
                result.revolutionCount = buffer.getInt() & 0xFFFFFFFF; // remove sign
                result.lastRevolutionTimeTicks = buffer.getShort() & 0xFFFF;
            }

            if(cadenceDataPresent){
                result.cadenceDataPresent = true;
                result.crankRevolutionCount = buffer.getInt();
                result.lastCrankRevolutionTimeTicks = buffer.getShort();
            }

            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format("Measurement revolutions: %d, time ticks %d", revolutionCount, lastRevolutionTimeTicks);
        }
    }

    public final static UUID UUID_CYCLING_SENSOR_SERVICE =
            UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_CYCLING_SENSOR_CSC_MEASUREMENT =
            UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    private static final Logger logger = LoggerFactory.getLogger(CyclingSensorSupport.class);

    private long persistenceInterval;
    private long nextPersistenceTimestamp = 0;

    private float wheelCircumferenceMeters;

    private CyclingSpeedCadenceMeasurement lastReportedMeasurement = null;
    private long lastMeasurementTime = 0;

    private BluetoothGattCharacteristic batteryCharacteristic = null;

    public CyclingSensorSupport() {
        super(logger);

        addSupportedService(UUID_CYCLING_SENSOR_SERVICE);
        addSupportedService(BatteryInfoProfile.SERVICE_UUID);
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config){
            case DeviceSettingsPreferenceConst.PREF_CYCLING_SENSOR_PERSISTENCE_INTERVAL:
            case DeviceSettingsPreferenceConst.PREF_CYCLING_SENSOR_WHEEL_DIAMETER:
                loadConfiguration();
                break;
        }
    }

    private void loadConfiguration(){
        Prefs deviceSpecificPrefs = new Prefs(
                GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress())
        );
        persistenceInterval = deviceSpecificPrefs.getInt(DeviceSettingsPreferenceConst.PREF_CYCLING_SENSOR_PERSISTENCE_INTERVAL, 60) * 1000;
        nextPersistenceTimestamp = 0;
        
        float wheelDiameter = deviceSpecificPrefs.getFloat(DeviceSettingsPreferenceConst.PREF_CYCLING_SENSOR_WHEEL_DIAMETER, 29);
        wheelCircumferenceMeters = (float)(wheelDiameter * 2.54 * Math.PI) / 100;
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        BluetoothGattCharacteristic measurementCharacteristic =
                getCharacteristic(UUID_CYCLING_SENSOR_CSC_MEASUREMENT);

        builder.add(new NotifyAction(measurementCharacteristic, true));

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        batteryCharacteristic = getCharacteristic(BatteryInfoProfile.UUID_CHARACTERISTIC_BATTERY_LEVEL);

        if(batteryCharacteristic != null){
            builder.add(new ReadAction(batteryCharacteristic));
        }

        loadConfiguration();

        gbDevice.setFirmwareVersion("1.0.0");

        return builder;
    }

    private void handleMeasurementCharacteristic(BluetoothGattCharacteristic characteristic){
        byte[] value = characteristic.getValue();
        if(value == null || value.length < 7){
            logger.error("Measurement characteristic value length smaller than 7");
            return;
        }

        CyclingSpeedCadenceMeasurement measurement = null;

        try {
            measurement = CyclingSpeedCadenceMeasurement.fromPayload(value);
        }catch (RuntimeException e){
            // do nothing, measurement stays null
        }

        if(measurement == null){
            return;
        }

        if(!measurement.revolutionDataPresent){
            return;
        }

        handleCyclingSpeedMeasurement(measurement);
    }

    private void handleCyclingSpeedMeasurement(CyclingSpeedCadenceMeasurement currentMeasurement) {
        logger.debug("Measurement " + currentMeasurement);

        long now = System.currentTimeMillis();

        Float speed = null;

        long lastMeasurementDelta = (now - lastMeasurementTime);

        if(lastMeasurementDelta <= 30_000){
            int ticksPassed = currentMeasurement.lastRevolutionTimeTicks - lastReportedMeasurement.lastRevolutionTimeTicks;
            // every second is subdivided in 1024 ticks
            int millisDelta = (int)(ticksPassed * (1000f / 1024f));

            if(millisDelta > 0) {
                int revolutionsDelta = currentMeasurement.revolutionCount - lastReportedMeasurement.revolutionCount;

                float revolutionsPerSecond = revolutionsDelta * (1000f / millisDelta);

                speed = revolutionsPerSecond * wheelCircumferenceMeters;
            }
        }

        lastReportedMeasurement = currentMeasurement;
        lastMeasurementTime = now;

        CyclingSample sample = new CyclingSample();

        if (currentMeasurement.revolutionDataPresent) {
            sample.setRevolutionCount(currentMeasurement.revolutionCount);
            sample.setSpeed(speed);
            sample.setDistance(currentMeasurement.revolutionCount * wheelCircumferenceMeters);
        }

        sample.setTimestamp(now);

        Intent liveIntent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES);
        liveIntent.putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
        liveIntent.putExtra("EXTRA_DEVICE_ADDRESS", getDevice().getAddress());
        LocalBroadcastManager.getInstance(getContext())
                .sendBroadcast(liveIntent);


        if(now < nextPersistenceTimestamp){
            // too early
            return;
        }

        nextPersistenceTimestamp = now + persistenceInterval;

        try(DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            CyclingSampleProvider sampleProvider =
                    new CyclingSampleProvider(getDevice(), session);

            Device databaseDevice = DBHelper.getDevice(getDevice(), session);
            User databaseUser = DBHelper.getUser(session);
            sample.setDevice(databaseDevice);
            sample.setUser(databaseUser);

            sampleProvider.addSample(sample);
        } catch (Exception e) {
            // throw new RuntimeException(e);
            logger.error("failed adding DB cycling sample");
        }
    }

    @Override
    public boolean onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        byte[] value = characteristic.getValue();

        if(characteristic.equals(batteryCharacteristic) && value != null && value.length == 1){
            GBDeviceEventBatteryInfo info = new GBDeviceEventBatteryInfo();
            info.level = characteristic.getValue()[0];
            handleGBDeviceEvent(info);
        }

        return true;
    }

    @Override
    public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if(characteristic.getUuid().equals(UUID_CYCLING_SENSOR_CSC_MEASUREMENT)){
            handleMeasurementCharacteristic(characteristic);
            return true;
        }
        return false;
    }
}
