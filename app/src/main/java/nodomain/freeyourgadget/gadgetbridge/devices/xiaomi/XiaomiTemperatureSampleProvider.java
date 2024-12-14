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
import nodomain.freeyourgadget.gadgetbridge.entities.XiaomiManualSample;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TemperatureSample;

public class XiaomiTemperatureSampleProvider implements TimeSampleProvider<TemperatureSample> {
    private final XiaomiManualSampleProvider manualSampleProvider;

    public XiaomiTemperatureSampleProvider(final GBDevice device, final DaoSession session) {
        manualSampleProvider = new XiaomiManualSampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<TemperatureSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<XiaomiManualSample> manualSamples = manualSampleProvider.getAllSamples(timestampFrom, timestampTo);

        final List<TemperatureSample> temperatureSamples = new ArrayList<>();

        for (final XiaomiManualSample manualSample : manualSamples) {
            if (XiaomiManualSampleProvider.TYPE_TEMPERATURE == manualSample.getType()) {
                temperatureSamples.add(new XiaomiTemperatureSample(manualSample));
            }
        }

        return temperatureSamples;
    }

    @Override
    public void addSample(final TemperatureSample timeSample) {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Override
    public void addSamples(final List<TemperatureSample> timeSamples) {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Override
    public TemperatureSample createSample() {
        throw new UnsupportedOperationException("read-only sample provider");
    }

    @Nullable
    @Override
    public TemperatureSample getLatestSample() {
        final XiaomiManualSample sample = manualSampleProvider.getLatestSample(XiaomiManualSampleProvider.TYPE_TEMPERATURE);
        if (sample != null) {
            return new XiaomiTemperatureSample(sample);
        }
        return null;
    }

    @Nullable
    @Override
    public TemperatureSample getLatestSample(final long until) {
        final XiaomiManualSample sample = manualSampleProvider.getLatestSample(XiaomiManualSampleProvider.TYPE_TEMPERATURE, until);
        if (sample != null) {
            return new XiaomiTemperatureSample(sample);
        }
        return null;
    }

    @Nullable
    @Override
    public TemperatureSample getFirstSample() {
        final XiaomiManualSample sample = manualSampleProvider.getFirstSample(XiaomiManualSampleProvider.TYPE_TEMPERATURE);
        if (sample != null) {
            return new XiaomiTemperatureSample(sample);
        }
        return null;
    }

    protected static class XiaomiTemperatureSample implements TemperatureSample {
        private final long timestamp;
        private final float temperature;

        public XiaomiTemperatureSample(final XiaomiManualSample sample) {
            this.timestamp = sample.getTimestamp();
            // first 2 bytes are body temperature
            // last 2 bytes are skin temperature
            // since the body temperature seems to always be 0, we only display skin temperature
            this.temperature = (sample.getValue() & 0xffff) / 100f;
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
