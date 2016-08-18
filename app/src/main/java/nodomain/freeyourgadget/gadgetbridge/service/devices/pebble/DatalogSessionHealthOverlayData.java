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

class DatalogSessionHealthOverlayData extends DatalogSessionPebbleHealth {

    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionHealthOverlayData.class);

    public DatalogSessionHealthOverlayData(byte id, UUID uuid, int tag, byte item_type, short item_size, GBDevice device) {
        super(id, uuid, tag, item_type, item_size, device);
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

        store(overlayRecords);
        return true;
    }

    private void store(OverlayRecord[] overlayRecords) {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            Long userId = DBHelper.getUser(session).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), session).getId();

            PebbleHealthActivityOverlayDao overlayDao = session.getPebbleHealthActivityOverlayDao();

            List<PebbleHealthActivityOverlay> overlayList = new ArrayList<>();
            for (OverlayRecord overlayRecord : overlayRecords) {
                overlayList.add(new PebbleHealthActivityOverlay(overlayRecord.timestampStart, overlayRecord.timestampStart + overlayRecord.durationSeconds - 1, deviceId, userId, overlayRecord.type)); //TODO: consider if "-1" is what we really want
            }
            overlayDao.insertOrReplaceInTx(overlayList);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
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