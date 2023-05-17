package nodomain.freeyourgadget.gadgetbridge.service.devices.bicycle_sensor.support;

import static nodomain.freeyourgadget.gadgetbridge.model.ActivityKind.TYPE_CYCLING;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.SharedPreferences;

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
import nodomain.freeyourgadget.gadgetbridge.devices.bicycle_sensor.db.CyclingSensorActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BicycleSensorActivitySample;
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

public class CyclingSensorSupport extends CyclingSensorBaseSupport {
    static class CyclingSpeedCadenceMeasurement {


        private static final int FLAG_REVOLUTION_DATA_PRESENT = 1 << 0;
        private static final int FLAG_CADENCE_DATA_PRESENT    = 1 << 1;
        public boolean revolutionDataPresent = false;
        public boolean cadenceDataPresent = false;
        public int revolutionCount;
        public int lastRevolutionTimeTicks;
        public long lastRevolutionTimeEpoch;
        private int crankRevolutionCount;
        private int lastCrankRevolutionTimeTicks;
        public long lastCrankRevolutionTimeEpoch;

        public static CyclingSpeedCadenceMeasurement fromPayload(byte[] payload){
            if(payload.length < 7){
                return null;
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

    public final static UUID UUID_BICYCLE_SENSOR_SERVICE =
            UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_BICYCLE_SENSOR_CSC_MEASUREMENT =
            UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    private static final Logger logger = LoggerFactory.getLogger(CyclingSensorSupport.class);

    private long persistenceInterval;
    private long nextPersistenceTimestamp = 0;

    private Device databaseDevice;
    private User databaseUser;

    private CyclingSpeedCadenceMeasurement lastReportedMeasurement = null;

    private BluetoothGattCharacteristic batteryCharacteristic = null;

    public CyclingSensorSupport() {
        super(logger);

        addSupportedService(UUID_BICYCLE_SENSOR_SERVICE);
        addSupportedService(BatteryInfoProfile.SERVICE_UUID);
    }

    private int getPersistenceInterval(){
        SharedPreferences deviceSpecificPrefs = GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress());
        return deviceSpecificPrefs.getInt(DeviceSettingsPreferenceConst.PREF_BICYCLE_SENSOR_PERSISTENCE_INTERVAL, 60);
    }

    @Override
    public void onSendConfiguration(String config) {
        switch (config){
            case DeviceSettingsPreferenceConst.PREF_BICYCLE_SENSOR_PERSISTENCE_INTERVAL:
                persistenceInterval = getPersistenceInterval();
                break;
        }
    }

    @Override
    protected TransactionBuilder initializeDevice(TransactionBuilder builder) {
        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZING, getContext()));

        BluetoothGattCharacteristic measurementCharacteristic =
                getCharacteristic(UUID_BICYCLE_SENSOR_CSC_MEASUREMENT);

        builder.add(new NotifyAction(measurementCharacteristic, true));

        builder.add(new SetDeviceStateAction(getDevice(), GBDevice.State.INITIALIZED, getContext()));
        batteryCharacteristic = getCharacteristic(BatteryInfoProfile.UUID_CHARACTERISTIC_BATTERY_LEVEL);

        if(batteryCharacteristic != null){
            builder.add(new ReadAction(batteryCharacteristic));
        }

        persistenceInterval = getPersistenceInterval();

        gbDevice.setFirmwareVersion("1.0.0");

        try(DBHandler handler = GBApplication.acquireDB()){
            DaoSession session = handler.getDaoSession();

            databaseDevice = DBHelper.getDevice(getDevice(), session);
            databaseUser = DBHelper.getUser(session);
        }catch (Exception ex){
            ex.printStackTrace();
        }

        return builder;
    }

    private void handleMeasurementCharacteristic(BluetoothGattCharacteristic characteristic){
        byte[] value = characteristic.getValue();
        if(value == null || value.length < 7){
            logger.error("Measurement characteristic value length smaller than 7");
            return;
        }

        CyclingSpeedCadenceMeasurement measurement = CyclingSpeedCadenceMeasurement.fromPayload(value);
        handleCyclingSpeedMeasurement(measurement);
    }

    private void handleCyclingSpeedMeasurement(CyclingSpeedCadenceMeasurement currentMeasurement) {
        logger.debug("Measurement " + currentMeasurement);

        long now = System.currentTimeMillis();


        try(DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            BicycleSensorActivitySample sample = new BicycleSensorActivitySample();
            CyclingSensorActivitySampleProvider sampleProvider =
                    new CyclingSensorActivitySampleProvider(getDevice(), session);

            boolean persistSample = currentMeasurement.revolutionDataPresent || currentMeasurement.cadenceDataPresent;

            if(!persistSample){
                return;
            }

            if (currentMeasurement.revolutionDataPresent) {
                sample.setRevolutionCount(currentMeasurement.revolutionCount);
                sample.setSteps(currentMeasurement.revolutionCount);
            }

            sample.setTimestamp((int)(now / 1000));
            sample.setDevice(databaseDevice);
            sample.setUser(databaseUser);
            sample.setRawKind(TYPE_CYCLING);

            Intent liveIntent = new Intent(DeviceService.ACTION_REALTIME_SAMPLES);
            liveIntent.putExtra(DeviceService.EXTRA_REALTIME_SAMPLE, sample);
            LocalBroadcastManager.getInstance(getContext())
                            .sendBroadcast(liveIntent);

            sampleProvider.addGBActivitySample(sample);
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        if(characteristic.getUuid().equals(UUID_BICYCLE_SENSOR_CSC_MEASUREMENT)){
            handleMeasurementCharacteristic(characteristic);
            return true;
        }
        return false;
    }
}
