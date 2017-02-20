package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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

    DatalogSessionHealthOverlayData(byte id, UUID uuid, int timestamp, int tag, byte item_type, short item_size, GBDevice device) {
        super(id, uuid, timestamp, tag, item_type, item_size, device);
        taginfo = "(Health - overlay data " + tag + " )";
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
        byte[] tempRecord = new byte[itemSize];

        for (int recordIdx = 0; recordIdx < recordCount; recordIdx++) {
            beginOfRecordPosition = initialPosition + recordIdx * itemSize;
            datalogMessage.position(beginOfRecordPosition);//we may not consume all the bytes of a record
            datalogMessage.get(tempRecord);
            overlayRecords[recordIdx] = new OverlayRecord(tempRecord);
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
                overlayList.add(new PebbleHealthActivityOverlay(overlayRecord.timestampStart, overlayRecord.timestampStart + overlayRecord.durationSeconds, overlayRecord.type, deviceId, userId, overlayRecord.getRawData()));
            }
            overlayDao.insertOrReplaceInTx(overlayList);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
    }

    private class OverlayRecord {
        byte[] knownVersions = {1, 3};
        short version;
        int type; //1=sleep, 2=deep sleep
        int offsetUTC; //probably
        int timestampStart;
        int durationSeconds;
        byte[] rawData;

        OverlayRecord(byte[] rawData) {
            this.rawData = rawData;
            ByteBuffer record = ByteBuffer.wrap(rawData);
            record.order(ByteOrder.LITTLE_ENDIAN);

            this.version = record.getShort();
            //TODO: check supported versions?
            record.getShort();//throwaway, unknown
            this.type = record.getShort();
            this.offsetUTC = record.getInt();
            this.timestampStart = record.getInt();
            this.durationSeconds = record.getInt();
        }

        byte[] getRawData() {
            if (storePebbleHealthRawRecord()) {
                return rawData;
            }
            return null;
        }
    }
}