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
import nodomain.freeyourgadget.gadgetbridge.model.Spo2Sample;

public class TestSpo2SampleProvider extends AbstractTestSampleProvider<Spo2Sample> {
    @NonNull
    @Override
    public List<Spo2Sample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<Spo2Sample> samples = new ArrayList<>();

        int spo2 = TestDeviceRand.randInt(timestampFrom, 85, 99);

        for (long ts = timestampFrom; ts < timestampTo; ts += 15 * 60 * 1000L) {
            if (TestDeviceRand.randBool(ts, 0.3f)) {
                samples.add(new TestSpo2Sample(ts, spo2));
            }
            spo2 += TestDeviceRand.randInt(ts, 85 - spo2, 99 - spo2);
        }

        return samples;
    }

    @Nullable
    @Override
    public Spo2Sample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestSpo2Sample(
                ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L),
                TestDeviceRand.randInt(ts, 85, 99)
        );
    }

    @Nullable
    @Override
    public Spo2Sample getFirstSample() {
        return new TestSpo2Sample(
                TestDeviceRand.BASE_TIMESTAMP,
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 85, 99)
        );
    }

    protected static class TestSpo2Sample implements Spo2Sample {
        private final long timestamp;
        private final int spo2;

        public TestSpo2Sample(final long timestamp, final int spo2) {
            this.timestamp = timestamp;
            this.spo2 = spo2;
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
        public int getSpo2() {
            return spo2;
        }
    }
}
