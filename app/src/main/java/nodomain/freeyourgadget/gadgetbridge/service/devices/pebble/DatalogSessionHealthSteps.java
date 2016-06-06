package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.HealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class DatalogSessionHealthSteps extends DatalogSessionPebbleHealth {

    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionHealthSteps.class);
    private final GBDevice device;

    public DatalogSessionHealthSteps(byte id, UUID uuid, int tag, byte item_type, short item_size, GBDevice device) {
        super(id, uuid, tag, item_type, item_size);
        taginfo = "(health - steps)";
        this.device = device;
    }

    @Override
    public boolean handleMessage(ByteBuffer datalogMessage, int length) {
        LOG.info("DATALOG " + taginfo + GB.hexdump(datalogMessage.array(), datalogMessage.position(), length));

        if (!isPebbleHealthEnabled()) {
            return false;
        }

        int timestamp;
        byte recordLength, recordNum;
        short recordVersion; //probably
        int beginOfPacketPosition, beginOfRecordPosition;

        int initialPosition = datalogMessage.position();
        if (0 != (length % itemSize))
            return false;//malformed message?

        int packetCount = length / itemSize;

        for (int packetIdx = 0; packetIdx < packetCount; packetIdx++) {
            beginOfPacketPosition = initialPosition + packetIdx * itemSize;
            datalogMessage.position(beginOfPacketPosition);//we may not consume all the records of a packet

            recordVersion = datalogMessage.getShort();

            if ((recordVersion != 5) && (recordVersion != 6))
                return false; //we don't know how to deal with the data TODO: this is not ideal because we will get the same message again and again since we NACK it

            timestamp = datalogMessage.getInt();
            datalogMessage.get(); //unknown, throw away
            recordLength = datalogMessage.get();
            recordNum = datalogMessage.get();

            beginOfRecordPosition = datalogMessage.position();
            StepsRecord[] stepsRecords = new StepsRecord[recordNum];

            for (int recordIdx = 0; recordIdx < recordNum; recordIdx++) {
                datalogMessage.position(beginOfRecordPosition + recordIdx * recordLength); //we may not consume all the bytes of a record
                stepsRecords[recordIdx] = new StepsRecord(timestamp, datalogMessage.get() & 0xff, datalogMessage.get() & 0xff, datalogMessage.getShort() & 0xffff, datalogMessage.get() & 0xff);
                timestamp += 60;
            }

            store(stepsRecords);
        }
        return true;//ACK by default
    }

    private void store(StepsRecord[] stepsRecords) {

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            HealthSampleProvider sampleProvider = new HealthSampleProvider(dbHandler.getDaoSession());
            PebbleActivitySample[] samples = new PebbleActivitySample[stepsRecords.length];
            // TODO: user and device
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(device, dbHandler.getDaoSession()).getId();
            for (int j = 0; j < stepsRecords.length; j++) {
                StepsRecord stepsRecord = stepsRecords[j];
                samples[j] = new PebbleActivitySample(
                        null,
                        stepsRecord.timestamp,
                        stepsRecord.intensity,
                        stepsRecord.steps,
                        sampleProvider.toRawActivityKind(ActivityKind.TYPE_ACTIVITY),
                        userId, deviceId);
            }

            sampleProvider.addGBActivitySamples(samples);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
    }

    private class StepsRecord {
        int timestamp;
        int steps;
        int orientation;
        int intensity;
        int light_intensity;

        public StepsRecord(int timestamp, int steps, int orientation, int intensity, int light_intensity) {
            this.timestamp = timestamp;
            this.steps = steps;
            this.orientation = orientation;
            this.intensity = intensity;
            this.light_intensity = light_intensity;
        }
    }

}