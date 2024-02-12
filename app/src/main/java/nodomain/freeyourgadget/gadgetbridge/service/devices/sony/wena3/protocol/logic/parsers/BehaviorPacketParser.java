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
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3ActivitySampleCombiner;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3ActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3BehaviorSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3BehaviorSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity.BehaviorSample;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.util.TimeUtil;

public class BehaviorPacketParser extends SamplePacketParser<BehaviorSample>  {
    private static final Logger LOG = LoggerFactory.getLogger(BehaviorPacketParser.class);
    public BehaviorPacketParser() {
        super(0x02);
    }

    @Override
    boolean tryExtractingMetadataFromHeaderBuffer(ByteBuffer buffer) {
        return true;
    }

    @Override
    BehaviorSample takeSampleFromBuffer(ByteBuffer buffer) {
        // Entry structure:
        // - 1b type
        // - 4b padding?
        // - 4b start date
        // - 4b end date
        int id = (buffer.get() & 0xFF);
        if(id < BehaviorSample.Type.LUT.length) {
            BehaviorSample.Type type = BehaviorSample.Type.LUT[id];
            buffer.position(buffer.position()+4);
            Date start = TimeUtil.wenaTimeToDate(buffer.getInt());
            Date end = TimeUtil.wenaTimeToDate(buffer.getInt());
            return new BehaviorSample(start, end, type);
        }
        return null;
    }

    @Override
    boolean canTakeSampleFromBuffer(ByteBuffer buffer) {
        return buffer.remaining() >= 13;
    }

    @Override
    public void finishReceiving(GBDevice device) {
        try (DBHandler db = GBApplication.acquireDB()) {
            SonyWena3BehaviorSampleProvider sampleProvider = new SonyWena3BehaviorSampleProvider(device, db.getDaoSession());
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
            List<Wena3BehaviorSample> samples = new ArrayList<>();

            for(BehaviorSample rawSample: accumulator) {
                Wena3BehaviorSample gbSample = new Wena3BehaviorSample();
                gbSample.setDeviceId(deviceId);
                gbSample.setUserId(userId);
                gbSample.setTimestamp(rawSample.start.getTime());
                gbSample.setTimestampFrom(rawSample.start.getTime());
                gbSample.setTimestampTo(rawSample.end.getTime());
                gbSample.setRawKind(rawSample.type.ordinal());
                samples.add(gbSample);
            }
            sampleProvider.addSamples(samples);

            if(!accumulator.isEmpty()) {
                SonyWena3ActivitySampleProvider activitySampleProvider = new SonyWena3ActivitySampleProvider(device, db.getDaoSession());
                SonyWena3ActivitySampleCombiner combiner = new SonyWena3ActivitySampleCombiner(activitySampleProvider);
                combiner.overlayBehaviorStartingAt(accumulator.get(0).start, sampleProvider);
            }
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording Behavior samples", e);
        }

        // Finally clean up the parser
        super.finishReceiving(device);
    }
}
