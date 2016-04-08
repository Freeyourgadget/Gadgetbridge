package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.HealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class DatalogSessionHealthSleep extends DatalogSession {

    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionHealthSleep.class);

    public DatalogSessionHealthSleep(byte id, UUID uuid, int tag, byte item_type, short item_size) {
        super(id, uuid, tag, item_type, item_size);
        taginfo = "(health - sleep " + tag + " )";
    }

    @Override
    public boolean handleMessage(ByteBuffer datalogMessage, int length) {
        LOG.info("DATALOG " + taginfo + GB.hexdump(datalogMessage.array(), datalogMessage.position(), length));
        switch (this.tag) {
            case 83:
                return handleMessage83(datalogMessage, length);
            case 84:
                return handleMessage84(datalogMessage, length);
            default:
                return false;
        }
    }

    private boolean handleMessage84(ByteBuffer datalogMessage, int length) {
        int initialPosition = datalogMessage.position();
        int beginOfRecordPosition;
        short recordVersion; //probably
        short recordType; //probably: 1=sleep, 2=deep sleep

        if (0 != (length % itemSize))
            return false;//malformed message?

        int recordCount = length / itemSize;
        SleepRecord84[] sleepRecords = new SleepRecord84[recordCount];

        for (int recordIdx = 0; recordIdx < recordCount; recordIdx++) {
            beginOfRecordPosition = initialPosition + recordIdx * itemSize;
            datalogMessage.position(beginOfRecordPosition);//we may not consume all the bytes of a record
            recordVersion = datalogMessage.getShort();
            if (recordVersion != 1)
                return false;//we don't know how to deal with the data TODO: this is not ideal because we will get the same message again and again since we NACK it

            datalogMessage.getShort();//throwaway, unknown
            recordType = datalogMessage.getShort();

            sleepRecords[recordIdx] = new SleepRecord84(recordType, datalogMessage.getInt(), datalogMessage.getInt(), datalogMessage.getInt());
        }

        return store84(sleepRecords);//NACK if we cannot store the data yet, the watch will send the sleep records again.
    }

    private boolean store84(SleepRecord84[] sleepRecords) {
        DBHandler dbHandler = null;
        SampleProvider sampleProvider = new HealthSampleProvider();
        try {
            dbHandler = GBApplication.acquireDB();
            int latestTimestamp = dbHandler.fetchLatestTimestamp(sampleProvider);
            for (SleepRecord84 sleepRecord : sleepRecords) {
                if (latestTimestamp < (sleepRecord.timestampStart + sleepRecord.durationSeconds))
                    return false;
                if (sleepRecord.type == 2) {
                    dbHandler.changeStoredSamplesType(sleepRecord.timestampStart, (sleepRecord.timestampStart + sleepRecord.durationSeconds), sampleProvider.toRawActivityKind(ActivityKind.TYPE_DEEP_SLEEP), sampleProvider);
                } else {
                    dbHandler.changeStoredSamplesType(sleepRecord.timestampStart, (sleepRecord.timestampStart + sleepRecord.durationSeconds), sampleProvider.toRawActivityKind(ActivityKind.TYPE_ACTIVITY), sampleProvider.toRawActivityKind(ActivityKind.TYPE_LIGHT_SLEEP), sampleProvider);
                }

            }
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        } finally {
            if (dbHandler != null) {
                dbHandler.release();
            }
        }
        return true;
    }

    private boolean handleMessage83(ByteBuffer datalogMessage, int length) {
        int initialPosition = datalogMessage.position();
        int beginOfRecordPosition;
        short recordVersion; //probably

        if (0 != (length % itemSize))
            return false;//malformed message?

        int recordCount = length / itemSize;
        SleepRecord83[] sleepRecords = new SleepRecord83[recordCount];

        for (int recordIdx = 0; recordIdx < recordCount; recordIdx++) {
            beginOfRecordPosition = initialPosition + recordIdx * itemSize;
            datalogMessage.position(beginOfRecordPosition);//we may not consume all the bytes of a record
            recordVersion = datalogMessage.getShort();
            if (recordVersion != 1)
                return false;//we don't know how to deal with the data TODO: this is not ideal because we will get the same message again and again since we NACK it

            sleepRecords[recordIdx] = new SleepRecord83(datalogMessage.getInt(),
                    datalogMessage.getInt(),
                    datalogMessage.getInt(),
                    datalogMessage.getInt());
        }

        return store83(sleepRecords);//NACK if we cannot store the data yet, the watch will send the sleep records again.
    }

    private boolean store83(SleepRecord83[] sleepRecords) {
        DBHandler dbHandler = null;
        SampleProvider sampleProvider = new HealthSampleProvider();
        GB.toast("Deep sleep is supported only from firmware 3.11 onwards.", Toast.LENGTH_LONG, GB.INFO);
        try {
            dbHandler = GBApplication.acquireDB();
            int latestTimestamp = dbHandler.fetchLatestTimestamp(sampleProvider);
            for (SleepRecord83 sleepRecord : sleepRecords) {
                if (latestTimestamp < sleepRecord.bedTimeEnd)
                    return false;
                dbHandler.changeStoredSamplesType(sleepRecord.bedTimeStart, sleepRecord.bedTimeEnd, sampleProvider.toRawActivityKind(ActivityKind.TYPE_ACTIVITY), sampleProvider.toRawActivityKind(ActivityKind.TYPE_LIGHT_SLEEP), sampleProvider);
            }
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        } finally {
            if (dbHandler != null) {
                dbHandler.release();
            }
        }
        return true;
    }

    private class SleepRecord83 {
        int offsetUTC; //probably
        int bedTimeStart;
        int bedTimeEnd;
        int deepSleepSeconds;

        public SleepRecord83(int offsetUTC, int bedTimeStart, int bedTimeEnd, int deepSleepSeconds) {
            this.offsetUTC = offsetUTC;
            this.bedTimeStart = bedTimeStart;
            this.bedTimeEnd = bedTimeEnd;
            this.deepSleepSeconds = deepSleepSeconds;
        }
    }

    private class SleepRecord84 {
        int type; //1=sleep, 2=deep sleep
        int offsetUTC; //probably
        int timestampStart;
        int durationSeconds;

        public SleepRecord84(int type, int offsetUTC, int timestampStart, int durationSeconds) {
            this.type = type;
            this.offsetUTC = offsetUTC;
            this.timestampStart = timestampStart;
            this.durationSeconds = durationSeconds;
        }
    }
}