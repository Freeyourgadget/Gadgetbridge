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
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;

public class TestPaiSampleProvider extends AbstractTestSampleProvider<PaiSample> {
    @NonNull
    @Override
    public List<PaiSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<PaiSample> samples = new ArrayList<>();

        int paiLow = TestDeviceRand.randInt(timestampFrom, 0, 15);
        int paiMod = TestDeviceRand.randInt(timestampFrom, 0, 15);
        int paiHigh = TestDeviceRand.randInt(timestampFrom, 0, 15);
        int paiTotal = paiLow + paiMod + paiHigh;

        for (long ts = timestampFrom; ts < timestampTo; ts += 12 * 60 * 60 * 1000L) {
            if (TestDeviceRand.randBool(ts, 0.75f)) {
                samples.add(new TestPaiSample(
                        ts,
                        paiLow,
                        paiMod,
                        paiHigh,
                        paiTotal
                ));
            }
            paiTotal += paiLow + paiMod + paiHigh;
            paiLow += TestDeviceRand.randInt(ts, - paiLow, 15 - paiLow);
            paiMod += TestDeviceRand.randInt(ts, - paiMod, 15 - paiMod);
            paiHigh += TestDeviceRand.randInt(ts, - paiHigh, 15 - paiHigh);
        }

        return samples;
    }

    @Nullable
    @Override
    public PaiSample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestPaiSample(
                ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L),
                TestDeviceRand.randInt(ts, 85, 99),
                TestDeviceRand.randInt(ts, 85, 99),
                TestDeviceRand.randInt(ts, 85, 99),
                TestDeviceRand.randInt(ts, 85, 99)
        );
    }

    @Nullable
    @Override
    public PaiSample getFirstSample() {
        return new TestPaiSample(
                TestDeviceRand.BASE_TIMESTAMP,
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 0, 15),
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 0, 15),
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 0, 15),
                TestDeviceRand.randInt(TestDeviceRand.BASE_TIMESTAMP, 70, 90)
        );
    }

    public static class TestPaiSample implements PaiSample {
        private final long timestamp;
        private final int paiLow;
        private final int paiModerate;
        private final int paiHigh;
        private final int paiTotal;

        public TestPaiSample(final long timestamp,
                             final int low,
                             final int moderate,
                             final int high,
                             final int total) {
            this.timestamp = timestamp;
            this.paiLow = low;
            this.paiModerate = moderate;
            this.paiHigh = high;
            this.paiTotal = total;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public float getPaiLow() {
            return paiLow;
        }

        @Override
        public float getPaiModerate() {
            return paiModerate;
        }

        @Override
        public float getPaiHigh() {
            return paiHigh;
        }

        @Override
        public int getTimeLow() {
            return paiLow;
        }

        @Override
        public int getTimeModerate() {
            return paiModerate;
        }

        @Override
        public int getTimeHigh() {
            return paiHigh;
        }

        @Override
        public float getPaiToday() {
            return paiLow + paiModerate + paiHigh;
        }

        @Override
        public float getPaiTotal() {
            return paiTotal;
        }
    }
}
