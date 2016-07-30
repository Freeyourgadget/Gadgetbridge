package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleHealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class DatalogSessionHealthSleep extends DatalogSessionPebbleHealth {

    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionHealthSleep.class);

    public DatalogSessionHealthSleep(byte id, UUID uuid, int tag, byte item_type, short item_size, GBDevice device) {
        super(id, uuid, tag, item_type, item_size, device);
        taginfo = "(health - sleep " + tag + " )";
    }

    @Override
    public boolean handleMessage(ByteBuffer datalogMessage, int length) {
        LOG.info("DATALOG " + taginfo + GB.hexdump(datalogMessage.array(), datalogMessage.position(), length));

        if (!isPebbleHealthEnabled()) {
            return false;
        }

        int initialPosition = datalogMessage.position();
        int beginOfRecordPosition;
        short recordVersion; //probably

        if (0 != (length % itemSize))
            return false;//malformed message?

        int recordCount = length / itemSize;
        SleepRecord[] sleepRecords = new SleepRecord[recordCount];

        for (int recordIdx = 0; recordIdx < recordCount; recordIdx++) {
            beginOfRecordPosition = initialPosition + recordIdx * itemSize;
            datalogMessage.position(beginOfRecordPosition);//we may not consume all the bytes of a record
            recordVersion = datalogMessage.getShort();
            if (recordVersion != 1)
                return false;//we don't know how to deal with the data TODO: this is not ideal because we will get the same message again and again since we NACK it

            sleepRecords[recordIdx] = new SleepRecord(datalogMessage.getInt(),
                    datalogMessage.getInt(),
                    datalogMessage.getInt(),
                    datalogMessage.getInt());
        }

        return store(sleepRecords);//NACK if we cannot store the data yet, the watch will send the sleep records again.
    }

    private boolean store(SleepRecord[] sleepRecords) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            SampleProvider sampleProvider = new PebbleHealthSampleProvider(getDevice(), dbHandler.getDaoSession());
            int latestTimestamp = sampleProvider.fetchLatestTimestamp();
            for (SleepRecord sleepRecord : sleepRecords) {
                if (latestTimestamp < sleepRecord.bedTimeEnd)
                    return false;
                sampleProvider.changeStoredSamplesType(sleepRecord.bedTimeStart, sleepRecord.bedTimeEnd, sampleProvider.toRawActivityKind(ActivityKind.TYPE_ACTIVITY), sampleProvider.toRawActivityKind(ActivityKind.TYPE_LIGHT_SLEEP));
            }
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
        return true;
    }

    private class SleepRecord {
        int offsetUTC; //probably
        int bedTimeStart;
        int bedTimeEnd;
        int deepSleepSeconds;

        public SleepRecord(int offsetUTC, int bedTimeStart, int bedTimeEnd, int deepSleepSeconds) {
            this.offsetUTC = offsetUTC;
            this.bedTimeStart = bedTimeStart;
            this.bedTimeEnd = bedTimeEnd;
            this.deepSleepSeconds = deepSleepSeconds;
        }
    }

}