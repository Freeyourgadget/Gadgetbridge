package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import android.database.sqlite.SQLiteDatabase;
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
        taginfo = "(health - sleep)";
    }

    @Override
    public boolean handleMessage(ByteBuffer datalogMessage, int length) {
        LOG.info("DATALOG " + taginfo + GB.hexdump(datalogMessage.array(), datalogMessage.position(), length));

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
        DBHandler dbHandler = null;
        SampleProvider sampleProvider = new HealthSampleProvider();
        GB.toast("We don't know how to store deep sleep from the pebble yet.", Toast.LENGTH_LONG, GB.INFO);
        try {
            dbHandler = GBApplication.acquireDB();
            int latestTimestamp = dbHandler.fetchLatestTimestamp(sampleProvider);
            for (SleepRecord sleepRecord : sleepRecords) {
                if (latestTimestamp < sleepRecord.bedTimeEnd)
                    return false;
                dbHandler.changeStoredSamplesType(sleepRecord.bedTimeStart, sleepRecord.bedTimeEnd, sampleProvider.toRawActivityKind(ActivityKind.TYPE_LIGHT_SLEEP), sampleProvider);
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