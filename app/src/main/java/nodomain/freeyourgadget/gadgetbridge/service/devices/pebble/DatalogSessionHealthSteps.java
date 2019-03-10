/*  Copyright (C) 2016-2019 0nse, Andreas Shimokawa, Carsten Pfeiffer,
    Daniele Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.pebble;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleHealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

class DatalogSessionHealthSteps extends DatalogSessionPebbleHealth {

    private static final Logger LOG = LoggerFactory.getLogger(DatalogSessionHealthSteps.class);

    DatalogSessionHealthSteps(byte id, UUID uuid, int timestamp, int tag, byte item_type, short item_size, GBDevice device) {
        super(id, uuid, timestamp, tag, item_type, item_size, device);
        taginfo = "(Health - steps)";
    }

    @Override
    public GBDeviceEvent[] handleMessage(ByteBuffer datalogMessage, int length) {
        LOG.info("DATALOG " + taginfo + GB.hexdump(datalogMessage.array(), datalogMessage.position(), length));

        if (!isPebbleHealthEnabled()) {
            return null;
        }

        int timestamp;
        byte recordLength, recordNum;
        short recordVersion; //probably
        int beginOfPacketPosition, beginOfRecordPosition;

        int initialPosition = datalogMessage.position();
        if (0 != (length % itemSize))
            return null;//malformed message?

        int packetCount = length / itemSize;

        for (int packetIdx = 0; packetIdx < packetCount; packetIdx++) {
            beginOfPacketPosition = initialPosition + packetIdx * itemSize;
            datalogMessage.position(beginOfPacketPosition);//we may not consume all the records of a packet

            recordVersion = datalogMessage.getShort();

            if ((recordVersion != 5) && (recordVersion != 6) && (recordVersion != 7) && (recordVersion != 12) && (recordVersion != 13))
                return null; //we don't know how to deal with the data TODO: this is not ideal because we will get the same message again and again since we NACK it

            timestamp = datalogMessage.getInt();
            datalogMessage.get(); //unknown, throw away
            recordLength = datalogMessage.get();
            recordNum = datalogMessage.get();

            beginOfRecordPosition = datalogMessage.position();
            StepsRecord[] stepsRecords = new StepsRecord[recordNum];
            byte[] tempRecord = new byte[recordLength];

            for (int recordIdx = 0; recordIdx < recordNum; recordIdx++) {
                datalogMessage.position(beginOfRecordPosition + recordIdx * recordLength); //we may not consume all the bytes of a record
                datalogMessage.get(tempRecord);
                stepsRecords[recordIdx] = new StepsRecord(timestamp, recordVersion, tempRecord);
                timestamp += 60;
            }

            store(stepsRecords);
        }
        return new GBDeviceEvent[]{null};//ACK by default
    }

    private void store(StepsRecord[] stepsRecords) {

        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            PebbleHealthSampleProvider sampleProvider = new PebbleHealthSampleProvider(getDevice(), dbHandler.getDaoSession());
            PebbleHealthActivitySample[] samples = new PebbleHealthActivitySample[stepsRecords.length];
            // TODO: user and device
            Long userId = DBHelper.getUser(dbHandler.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(getDevice(), dbHandler.getDaoSession()).getId();
            for (int j = 0; j < stepsRecords.length; j++) {
                StepsRecord stepsRecord = stepsRecords[j];
                samples[j] = new PebbleHealthActivitySample(
                        stepsRecord.timestamp,
                        deviceId, userId,
                        stepsRecord.getRawData(),
                        stepsRecord.intensity,
                        stepsRecord.steps,
                        stepsRecord.heart_rate
                );
                samples[j].setProvider(sampleProvider);
            }

            sampleProvider.addGBActivitySamples(samples);
        } catch (Exception ex) {
            LOG.debug(ex.getMessage());
        }
    }

    private class StepsRecord {
        byte[] knownVersions = {5, 6, 7, 12, 13};
        short version;
        int timestamp;
        int steps;
        int orientation;
        int intensity;
        int light_intensity;
        int heart_rate;

        byte[] rawData;

        StepsRecord(int timestamp, short version, byte[] rawData) {
            this.timestamp = timestamp;
            this.rawData = rawData;
            ByteBuffer record = ByteBuffer.wrap(rawData);
            record.order(ByteOrder.LITTLE_ENDIAN);

            this.version = version;
            //TODO: check supported versions?

            this.steps = record.get() & 0xff;
            this.orientation = record.get() & 0xff;
            this.intensity = record.getShort() & 0xffff;
            this.light_intensity = record.get() & 0xff;
            if (version >= 7) {
                // skip 7 bytes
                record.getInt();
                record.getShort();
                record.get();
                this.heart_rate = record.get() & 0xff;
            }
        }

        byte[] getRawData() {
            if (storePebbleHealthRawRecord()) {
                return rawData;
            }
            return null;
        }
    }

}