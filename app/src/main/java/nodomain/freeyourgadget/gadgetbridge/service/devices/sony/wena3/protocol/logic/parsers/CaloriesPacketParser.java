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
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3CaloriesSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3CaloriesSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class CaloriesPacketParser extends LinearSamplePacketParser<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(CaloriesPacketParser.class);
    public CaloriesPacketParser() {
        super(0x06, OneBytePerSamplePacketParser.ONE_MINUTE_IN_MS);
    }

    @Override
    Integer takeSampleFromBuffer(ByteBuffer buffer) {
        return (buffer.getShort() & 65535);
    }

    @Override
    boolean canTakeSampleFromBuffer(ByteBuffer buffer) {
        return buffer.remaining() >= 2;
    }
    @Override
    public void finishReceiving(GBDevice device) {
        try (DBHandler db = GBApplication.acquireDB()) {
            SonyWena3CaloriesSampleProvider sampleProvider = new SonyWena3CaloriesSampleProvider(device, db.getDaoSession());
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();

            int i = 0;
            List<Wena3CaloriesSample> samples = new ArrayList<>();

            for(int rawSample: accumulator) {
                Date currentSampleDate = timestampOfSampleAtIndex(i);

                Wena3CaloriesSample gbSample = new Wena3CaloriesSample();
                gbSample.setDeviceId(deviceId);
                gbSample.setUserId(userId);
                gbSample.setTimestamp(currentSampleDate.getTime());
                gbSample.setCalories(rawSample);
                samples.add(gbSample);

                i++;
            }

            sampleProvider.addSamples(samples);
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording Calories samples", e);
        }

        // Finally clean up the parser
        super.finishReceiving(device);
    }
}
