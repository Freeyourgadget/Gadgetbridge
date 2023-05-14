package nodomain.freeyourgadget.gadgetbridge.service.devices.bicycle_sensor.support;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.SharedPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.bicycle_sensor.db.BicycleSensorActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.BicycleSensorActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Measurement;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.NotifyAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.SetDeviceStateAction;

public class BicycleSensorSupport extends BicycleSensorBaseSupport{
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
                result.revolutionCount = buffer.getInt();
                result.lastRevolutionTimeTicks = buffer.getShort();
            }

            if(cadenceDataPresent){
                result.cadenceDataPresent = true;
                result.crankRevolutionCount = buffer.getInt();
                result.lastCrankRevolutionTimeTicks = buffer.getShort();
            }

            return result;
        }
    }

    public final static UUID UUID_BICYCLE_SENSOR_SERVICE =
            UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    public final static UUID UUID_BICYCLE_SENSOR_CSC_MEASUREMENT =
            UUID.fromString("00002a5b-0000-1000-8000-00805f9b34fb");

    private static final Logger logger = LoggerFactory.getLogger(BicycleSensorSupport.class);

    private long persistenceInterval;
    private long nextPersistenceTimestamp = 0;

    private Device databaseDevice;
    private User databaseUser;

    private CyclingSpeedCadenceMeasurement lastReportedMeasurement = null;

    public BicycleSensorSupport() {
        super(logger);

        persistenceInterval = getPersistenceInterval();
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

        try(DBHandler handler = GBApplication.acquireDB()){
            DaoSession session = handler.getDaoSession();

            databaseDevice = DBHelper.getDevice(getDevice(), session);
            databaseUser = DBHelper.getUser(session);
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            GBApplication.releaseDB();
        }

        return builder;
    }

    private void handleMeasurementCharacteristic(BluetoothGattCharacteristic characteristic){
        logger.debug(characteristic.getUuid().toString());
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

        if(lastReportedMeasurement == null){
            // since we need a delta we will ignore an uninitialized value
            currentMeasurement.lastRevolutionTimeEpoch = now;
            currentMeasurement.lastCrankRevolutionTimeEpoch = now;
            lastReportedMeasurement = currentMeasurement;
            return;
        }

        if(now < nextPersistenceTimestamp){
            return;
        }

        try(DBHandler handler = GBApplication.acquireDB()) {
            DaoSession session = handler.getDaoSession();

            BicycleSensorActivitySample sample = new BicycleSensorActivitySample();
            BicycleSensorActivitySampleProvider sampleProvider =
                    new BicycleSensorActivitySampleProvider(getDevice(), session);
            if (currentMeasurement.revolutionDataPresent) {
                sample.setRevolutionCount(currentMeasurement.revolutionCount);

                long revolutionTimeDeltaEpoch = now - lastReportedMeasurement.lastRevolutionTimeEpoch;

                if(revolutionTimeDeltaEpoch < (45 * 1000)){
                    // if the reporting delta is withing 45 secs
                    // we can use the timestamp send by the sensor
                    // to calculate the new time
                    int deltaTicks = currentMeasurement.lastRevolutionTimeTicks - lastReportedMeasurement.lastRevolutionTimeTicks;

                    if(deltaTicks < 0){
                        // handle ticks rollover
                        // adding only once should be enough since out timeframe is only 45 secs...
                        deltaTicks += (1 << 16);
                    }
                    // deltaTicks should be positive now and account for rollover aswell...
                    // the sensor divides a second in 1024 ticks, hence we need to recalculate
                    long deltaMilliSeconds = (long) (1000 * (deltaTicks / 1024f));
                }

                sample.setDevice(databaseDevice);
                sample.setUser(databaseUser);
            }

            sampleProvider.addGBActivitySample(sample);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            GBApplication.releaseDB();
        }

        nextPersistenceTimestamp = now + persistenceInterval;
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
