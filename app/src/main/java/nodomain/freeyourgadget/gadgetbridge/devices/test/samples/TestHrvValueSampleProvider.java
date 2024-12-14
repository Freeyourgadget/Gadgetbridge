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
import nodomain.freeyourgadget.gadgetbridge.model.HrvValueSample;

public class TestHrvValueSampleProvider extends AbstractTestSampleProvider<HrvValueSample> {
    @NonNull
    @Override
    public List<HrvValueSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<HrvValueSample> samples = new ArrayList<>();

        int hrv = TestDeviceRand.randInt(timestampFrom, 50, 95);

        for (long ts = timestampFrom; ts < timestampTo; ts += 5 * 60 * 1000L) {
            samples.add(new TestHrvValueSample(ts, hrv));
            hrv += TestDeviceRand.randInt(ts, (10 - hrv) / 10, (90 - hrv) / 10);
        }

        return samples;
    }

    @Nullable
    @Override
    public HrvValueSample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestHrvValueSample(
                ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L),
                TestDeviceRand.randInt(ts, 50, 95)
        );
    }

    @Nullable
    @Override
    public HrvValueSample getFirstSample() {
        return new TestHrvValueSample(
                TestDeviceRand.BASE_TIMESTAMP,
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 50, 95)
        );
    }

    protected static class TestHrvValueSample implements HrvValueSample {
        private final long timestamp;
        private final int hrv;

        public TestHrvValueSample(final long timestamp, final int hrv) {
            this.timestamp = timestamp;
            this.hrv = hrv;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public int getValue() {
            return hrv;
        }
    }
}
