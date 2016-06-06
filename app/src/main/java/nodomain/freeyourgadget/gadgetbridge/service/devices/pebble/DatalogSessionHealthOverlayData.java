package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

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

class DatalogSessionHealthOverlayData extends DatalogSessionPebbleHealth {

    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionHealthOverlayData.class);

    public DatalogSessionHealthOverlayData(byte id, UUID uuid, int tag, byte item_type, short item_size) {
        super(id, uuid, tag, item_type, item_size);
        taginfo = "(health - overlay data " + tag + " )";
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
        short recordType; //probably: 1=sleep, 2=deep sleep, 5=??run??ignored for now

        if (0 != (length % itemSize))
            return false;//malformed message?

        int recordCount = length / itemSize;
        OverlayRecord[] overlayRecords = new OverlayRecord[recordCount];

        for (int recordIdx = 0; recordIdx < recordCount; recordIdx++) {
            beginOfRecordPosition = initialPosition + recordIdx * itemSize;
            datalogMessage.position(beginOfRecordPosition);//we may not consume all the bytes of a record
            recordVersion = datalogMessage.getShort();
            if ((recordVersion != 1) && (recordVersion != 3))
                return false;//we don't know how to deal with the data TODO: this is not ideal because we will get the same message again and again since we NACK it

            datalogMessage.getShort();//throwaway, unknown
            recordType = datalogMessage.getShort();

            overlayRecords[recordIdx] = new OverlayRecord(recordType, datalogMessage.getInt(), datalogMessage.getInt(), datalogMessage.getInt());
        }

        return store(overlayRecords);//NACK if we cannot store the data yet, the watch will send the overlay records again.
    }

    private boolean store(OverlayRecord[] overlayRecords) {
        DBHandler dbHandler = null;
        SampleProvider sampleProvider = new HealthSampleProvider();
        try {
            dbHandler = GBApplication.acquireDB();
            int latestTimestamp = dbHandler.fetchLatestTimestamp(sampleProvider);
            for (OverlayRecord overlayRecord : overlayRecords) {
                if (latestTimestamp < (overlayRecord.timestampStart + overlayRecord.durationSeconds))
                    return false;
                switch (overlayRecord.type) {
                    case 1:
                        dbHandler.changeStoredSamplesType(overlayRecord.timestampStart, (overlayRecord.timestampStart + overlayRecord.durationSeconds), sampleProvider.toRawActivityKind(ActivityKind.TYPE_ACTIVITY), sampleProvider.toRawActivityKind(ActivityKind.TYPE_LIGHT_SLEEP), sampleProvider);
                        break;
                    case 2:
                        dbHandler.changeStoredSamplesType(overlayRecord.timestampStart, (overlayRecord.timestampStart + overlayRecord.durationSeconds), sampleProvider.toRawActivityKind(ActivityKind.TYPE_DEEP_SLEEP), sampleProvider);
                        break;
                    default:
                        //TODO: other values refer to unknown activity types.
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

    private class OverlayRecord {
        int type; //1=sleep, 2=deep sleep
        int offsetUTC; //probably
        int timestampStart;
        int durationSeconds;

        public OverlayRecord(int type, int offsetUTC, int timestampStart, int durationSeconds) {
            this.type = type;
            this.offsetUTC = offsetUTC;
            this.timestampStart = timestampStart;
            this.durationSeconds = durationSeconds;
        }
    }
}