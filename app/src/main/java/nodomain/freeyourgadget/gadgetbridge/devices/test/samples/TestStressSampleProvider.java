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
import nodomain.freeyourgadget.gadgetbridge.model.StressSample;

public class TestStressSampleProvider extends AbstractTestSampleProvider<StressSample> {
    @NonNull
    @Override
    public List<StressSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<StressSample> samples = new ArrayList<>();

        int stress = TestDeviceRand.randInt(timestampFrom, 10, 90);

        for (long ts = timestampFrom; ts < timestampTo; ts += 15 * 60 * 1000L) {
            if (TestDeviceRand.randBool(ts, 0.3f)) {
                samples.add(new TestStressSample(ts, stress));
            }
            stress += TestDeviceRand.randInt(ts, (10 - stress) / 10, (90 - stress) / 10);
        }

        return samples;
    }

    @Nullable
    @Override
    public StressSample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestStressSample(
                ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L),
                TestDeviceRand.randInt(ts, 10, 90)
        );
    }

    @Nullable
    @Override
    public StressSample getFirstSample() {
        return new TestStressSample(
                TestDeviceRand.BASE_TIMESTAMP,
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 10, 90)
        );
    }

    protected static class TestStressSample implements StressSample {
        private final long timestamp;
        private final int stress;

        public TestStressSample(final long timestamp, final int stress) {
            this.timestamp = timestamp;
            this.stress = stress;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Type getType() {
            return Type.UNKNOWN;
        }

        @Override
        public int getStress() {
            return stress;
        }
    }
}
