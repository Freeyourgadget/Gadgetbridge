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
import nodomain.freeyourgadget.gadgetbridge.model.HrvSummarySample;

public class TestHrvSummarySampleProvider extends AbstractTestSampleProvider<HrvSummarySample> {
    @NonNull
    @Override
    public List<HrvSummarySample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<HrvSummarySample> samples = new ArrayList<>();

        for (long ts = timestampFrom; ts < timestampTo; ts += 24 * 60 * 60 * 1000L) {
            samples.add(new TestHrvSummarySample(ts));
        }

        return samples;
    }

    @Nullable
    @Override
    public HrvSummarySample getLatestSample() {
        final long ts = System.currentTimeMillis();
        return new TestHrvSummarySample(ts - TestDeviceRand.randLong(ts, 10 * 1000L, 2 * 60 * 60 * 1000L));
    }

    @Nullable
    @Override
    public HrvSummarySample getFirstSample() {
        return new TestHrvSummarySample(TestDeviceRand.BASE_TIMESTAMP);
    }

    protected static class TestHrvSummarySample implements HrvSummarySample {
        private final long timestamp;
        private final int weeklyAverage;

        public TestHrvSummarySample(final long timestamp) {
            this(timestamp, TestDeviceRand.randInt(timestamp, 30, 95));
        }

        public TestHrvSummarySample(final long timestamp, final int weeklyAverage) {
            this.timestamp = timestamp;
            this.weeklyAverage = weeklyAverage;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public Integer getWeeklyAverage() {
            return weeklyAverage;
        }

        @Override
        public Integer getLastNightAverage() {
            return TestDeviceRand.randInt(timestamp, 55, 90);
        }

        @Override
        public Integer getLastNight5MinHigh() {
            return TestDeviceRand.randInt(timestamp, 90, 110);
        }

        @Override
        public Integer getBaselineLowUpper() {
            return 50;
        }

        @Override
        public Integer getBaselineBalancedLower() {
            return 65;
        }

        @Override
        public Integer getBaselineBalancedUpper() {
            return 80;
        }

        @Override
        public Status getStatus() {
            if (getWeeklyAverage() > getBaselineBalancedUpper()) {
                return Status.UNBALANCED;
            } else if (getWeeklyAverage() < getBaselineLowUpper()) {
                return Status.POOR;
            } else if (getWeeklyAverage() < getBaselineBalancedLower()) {
                return Status.LOW;
            }

            return Status.BALANCED;
        }
    }
}
