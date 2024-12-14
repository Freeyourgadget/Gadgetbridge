/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.xiaomi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.TimeSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiDailySummarySample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;

public class XiaomiHeartRateRestingSampleProvider implements TimeSampleProvider<HeartRateSample> {
    private final XiaomiDailySummarySampleProvider dailySummarySampleProvider;

    public XiaomiHeartRateRestingSampleProvider(final GBDevice device, final DaoSession session) {
        this.dailySummarySampleProvider = new XiaomiDailySummarySampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<HeartRateSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<XiaomiDailySummarySample> allSamples = dailySummarySampleProvider.getAllSamples(timestampFrom, timestampTo);
        final List<HeartRateSample> ret = new ArrayList<>(allSamples.size());
        for (final XiaomiDailySummarySample sample : allSamples) {
            ret.add(new XiaomiHeartRateRestingSample(sample));
        }
        return ret;
    }

    @Override
    public void addSample(final HeartRateSample timeSample) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public void addSamples(final List<HeartRateSample> timeSamples) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public HeartRateSample createSample() {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Nullable
    @Override
    public HeartRateSample getLatestSample() {
        final XiaomiDailySummarySample sample = dailySummarySampleProvider.getLatestSample();
        if (sample != null) {
            return new XiaomiHeartRateRestingSample(sample);
        }
        return null;
    }

    @Nullable
    @Override
    public HeartRateSample getLatestSample(final long until) {
        final XiaomiDailySummarySample sample = dailySummarySampleProvider.getLatestSample(until);
        if (sample != null) {
            return new XiaomiHeartRateRestingSample(sample);
        }
        return null;
    }

    @Nullable
    @Override
    public HeartRateSample getFirstSample() {
        final XiaomiDailySummarySample sample = dailySummarySampleProvider.getFirstSample();
        if (sample != null) {
            return new XiaomiHeartRateRestingSample(sample);
        }
        return null;
    }

    public static class XiaomiHeartRateRestingSample implements HeartRateSample {
        private final long timestamp;
        private final int heartRate;

        public XiaomiHeartRateRestingSample(final XiaomiDailySummarySample sample) {
            this.timestamp = sample.getTimestamp();
            this.heartRate = sample.getHrResting();
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public int getHeartRate() {
            return heartRate;
        }
    }
}
