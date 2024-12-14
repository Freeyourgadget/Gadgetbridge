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
package nodomain.freeyourgadget.gadgetbridge.devices.test.samples;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.test.TestDeviceRand;
import nodomain.freeyourgadget.gadgetbridge.model.BodyEnergySample;

public class TestBodyEnergySampleProvider extends AbstractTestSampleProvider<BodyEnergySample> {
    @NonNull
    @Override
    public List<BodyEnergySample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<BodyEnergySample> samples = new ArrayList<>();

        for (long ts = timestampFrom; ts < timestampTo; ts += 15 * 60 * 1000L) {
            samples.add(new TestBodyEnergySample(ts));
        }

        return samples;
    }

    @Nullable
    @Override
    public BodyEnergySample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestBodyEnergySample(ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L));
    }

    @Nullable
    @Override
    public BodyEnergySample getFirstSample() {
        return new TestBodyEnergySample(TestDeviceRand.BASE_TIMESTAMP);
    }

    protected static class TestBodyEnergySample implements BodyEnergySample {
        private final long timestamp;

        public TestBodyEnergySample(final long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public int getEnergy() {
            return (int) Math.round(45 * Math.sin(timestamp / (86400000 / (2 * Math.PI))) + 55);
        }
    }
}
