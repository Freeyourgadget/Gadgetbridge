package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
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

        store(sleepRecords);
        return true;
    }

    private void store(SleepRecord[] sleepRecords) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), session).getId();

            PebbleHealthActivityOverlayDao overlayDao = session.getPebbleHealthActivityOverlayDao();

            List<PebbleHealthActivityOverlay> overlayList = new ArrayList<>();
            for (SleepRecord sleepRecord : sleepRecords) {
                overlayList.add(new PebbleHealthActivityOverlay(null, sleepRecord.bedTimeStart, sleepRecord.bedTimeEnd - 1, sleepRecord.type, userId, deviceId)); //TODO: consider if "-1" is what we really want
            }
            overlayDao.insertOrReplaceInTx(overlayList);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
    }

    private class SleepRecord {
        int type = 1; //sleep, hardcoded as we don't get other info
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