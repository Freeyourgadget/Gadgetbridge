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
import nodomain.freeyourgadget.gadgetbridge.model.RespiratoryRateSample;

public class TestRespiratoryRateSampleProvider extends AbstractTestSampleProvider<RespiratoryRateSample> {
    @NonNull
    @Override
    public List<RespiratoryRateSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<RespiratoryRateSample> samples = new ArrayList<>();

        for (long ts = timestampFrom; ts < timestampTo; ts += 15 * 60 * 1000L) {
            samples.add(new TestRespiratoryRateSample(ts));
        }

        return samples;
    }

    @Nullable
    @Override
    public RespiratoryRateSample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestRespiratoryRateSample(ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L));
    }

    @Nullable
    @Override
    public RespiratoryRateSample getFirstSample() {
        return new TestRespiratoryRateSample(TestDeviceRand.BASE_TIMESTAMP);
    }

    protected static class TestRespiratoryRateSample implements RespiratoryRateSample {
        private final long timestamp;

        public TestRespiratoryRateSample(final long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public float getRespiratoryRate() {
            return TestDeviceRand.randFloat(timestamp, 10, 15);
        }
    }
}
