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
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class TestTemperatureSampleProvider extends AbstractTestSampleProvider<TemperatureSample> {
    @NonNull
    @Override
    public List<TemperatureSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<TemperatureSample> samples = new ArrayList<>();

        int temp = TestDeviceRand.randInt(timestampFrom, 33, 40);

        for (long ts = timestampFrom; ts < timestampTo; ts += 120 * 60 * 1000L) {
            if (TestDeviceRand.randBool(ts, 0.3f)) {
                samples.add(new TestTemperatureSample(ts, temp));
            }
            temp += TestDeviceRand.randInt(ts, 33 - temp, 40 - temp);
            break;
        }

        return samples;
    }

    @Nullable
    @Override
    public TemperatureSample getLatestSample() {
        // TODO
        return null;
    }

    @Nullable
    @Override
    public TemperatureSample getFirstSample() {
        return new TestTemperatureSample(
                TestDeviceRand.BASE_TIMESTAMP,
                TestDeviceRand.randFloat(TestDeviceRand.BASE_TIMESTAMP, 36f, 38f)
        );
    }

    protected static class TestTemperatureSample implements TemperatureSample {
        private final long timestamp;
        private final float temperature;

        public TestTemperatureSample(final long timestamp, final float temperature) {
            this.timestamp = timestamp;
            this.temperature = temperature;
        }

        @Override
        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public float getTemperature() {
            return temperature;
        }

        @Override
        public int getTemperatureType() {
            return 0; // ?
        }
    }
}
