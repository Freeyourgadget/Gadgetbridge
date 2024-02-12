/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.parsers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3Vo2SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3Vo2Sample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.Vo2MaxSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.util.TimeUtil;

public class Vo2MaxPacketParser extends SamplePacketParser<Vo2MaxSample> {
    private static final Logger LOG = LoggerFactory.getLogger(Vo2MaxPacketParser.class);
    public Vo2MaxPacketParser() {
        super(0x03);
    }

    @Override
    Vo2MaxSample takeSampleFromBuffer(ByteBuffer buffer) {
        Date ts = TimeUtil.wenaTimeToDate(buffer.getInt());
        int value = buffer.get() & 0xFF;
        return new Vo2MaxSample(ts, value);
    }

    @Override
    boolean canTakeSampleFromBuffer(ByteBuffer buffer) {
        return buffer.remaining() >= 5;
    }

    @Override
    boolean tryExtractingMetadataFromHeaderBuffer(ByteBuffer buffer) {
        return true;
    }

    @Override
    public void finishReceiving(GBDevice device) {
        try (DBHandler db = GBApplication.acquireDB()) {
            SonyWena3Vo2SampleProvider sampleProvider = new SonyWena3Vo2SampleProvider(device, db.getDaoSession());
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
            List<Wena3Vo2Sample> samples = new ArrayList<>();
            Date currentSampleDate = null;
            int currentDateDatapoint = 0;

            for(Vo2MaxSample rawSample: accumulator) {
                if(currentSampleDate == null || currentSampleDate != rawSample.timestamp) {
                    currentDateDatapoint = 0;
                    currentSampleDate = rawSample.timestamp;
                } else {
                    currentDateDatapoint ++;
                }
                Wena3Vo2Sample gbSample = new Wena3Vo2Sample();
                gbSample.setDeviceId(deviceId);
                gbSample.setUserId(userId);
                gbSample.setTimestamp(currentSampleDate.getTime());
                gbSample.setDatapoint(currentDateDatapoint);
                gbSample.setVo2(rawSample.value);
                samples.add(gbSample);
            }
            sampleProvider.addSamples(samples);
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording Vo2 samples", e);
        }

        // Finally clean up the parser
        super.finishReceiving(device);
    }
}
