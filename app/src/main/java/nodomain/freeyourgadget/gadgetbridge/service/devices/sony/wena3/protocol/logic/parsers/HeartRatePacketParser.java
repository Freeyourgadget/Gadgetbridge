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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3ActivitySampleCombiner;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3ActivitySampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.sony.wena3.SonyWena3HeartRateSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.Wena3HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class HeartRatePacketParser extends OneBytePerSamplePacketParser {
    private static final Logger LOG = LoggerFactory.getLogger(HeartRatePacketParser.class);
    private static final int HEART_PKT_MARKER = 0x01;
    public HeartRatePacketParser() {
        super(HEART_PKT_MARKER, ONE_MINUTE_IN_MS);
    }

    @Override
    public void finishReceiving(GBDevice device) {
        try (DBHandler db = GBApplication.acquireDB()) {
            SonyWena3HeartRateSampleProvider sampleProvider = new SonyWena3HeartRateSampleProvider(device, db.getDaoSession());
            Long userId = DBHelper.getUser(db.getDaoSession()).getId();
            Long deviceId = DBHelper.getDevice(device, db.getDaoSession()).getId();
            List<Wena3HeartRateSample> samples = new ArrayList<>();

            Date currentSampleDate = startDate;
            int i = 0;
            for(int rawSample: accumulator) {
                Wena3HeartRateSample gbSample = new Wena3HeartRateSample();
                gbSample.setDeviceId(deviceId);
                gbSample.setUserId(userId);
                gbSample.setTimestamp(currentSampleDate.getTime());
                gbSample.setHeartRate(rawSample);
                samples.add(gbSample);

                i++;
                currentSampleDate = timestampOfSampleAtIndex(i);
            }

            sampleProvider.addSamples(samples);

            if(!accumulator.isEmpty()) {
                SonyWena3ActivitySampleProvider activitySampleProvider = new SonyWena3ActivitySampleProvider(device, db.getDaoSession());
                SonyWena3ActivitySampleCombiner combiner = new SonyWena3ActivitySampleCombiner(activitySampleProvider);
                combiner.overlayHeartRateStartingAt(startDate, sampleProvider);
            }
        } catch (Exception e) {
            LOG.error("Error acquiring database for recording heart rate samples", e);
        }

        // Finally clean up the parser
        super.finishReceiving(device);
    }
}
