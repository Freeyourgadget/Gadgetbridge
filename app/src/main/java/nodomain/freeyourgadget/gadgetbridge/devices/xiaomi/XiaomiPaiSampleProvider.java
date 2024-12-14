/*  Copyright (C) 2023-2024 José Rebelo

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
import nodomain.freeyourgadget.gadgetbridge.model.PaiSample;

public class XiaomiPaiSampleProvider implements TimeSampleProvider<PaiSample> {
    private final XiaomiDailySummarySampleProvider dailySummarySampleProvider;

    public XiaomiPaiSampleProvider(final GBDevice device, final DaoSession session) {
        this.dailySummarySampleProvider = new XiaomiDailySummarySampleProvider(device, session);
    }

    @NonNull
    @Override
    public List<PaiSample> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<XiaomiDailySummarySample> allSamples = dailySummarySampleProvider.getAllSamples(timestampFrom, timestampTo);
        final List<PaiSample> ret = new ArrayList<>(allSamples.size());
        for (final XiaomiDailySummarySample sample : allSamples) {
            ret.add(new XiaomiPaiSample(sample));
        }
        return ret;
    }

    @Override
    public void addSample(final PaiSample timeSample) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public void addSamples(final List<PaiSample> timeSamples) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public PaiSample createSample() {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Nullable
    @Override
    public PaiSample getLatestSample() {
        final XiaomiDailySummarySample sample = dailySummarySampleProvider.getLatestSample();
        if (sample != null) {
            return new XiaomiPaiSample(sample);
        }
        return null;
    }

    @Nullable
    @Override
    public PaiSample getLatestSample(final long until) {
        final XiaomiDailySummarySample sample = dailySummarySampleProvider.getLatestSample(until);
        if (sample != null) {
            return new XiaomiPaiSample(sample);
        }
        return null;
    }

    @Nullable
    @Override
    public PaiSample getFirstSample() {
        final XiaomiDailySummarySample sample = dailySummarySampleProvider.getFirstSample();
        if (sample != null) {
            return new XiaomiPaiSample(sample);
        }
        return null;
    }

    public static class XiaomiPaiSample implements PaiSample {
        private final long timestamp;
        private final int paiLow;
        private final int paiModerate;
        private final int paiHigh;
        private final int paiTotal;

        public XiaomiPaiSample(final XiaomiDailySummarySample sample) {
            this.timestamp = sample.getTimestamp();
            this.paiLow = sample.getVitalityIncreaseLight();
            this.paiModerate = sample.getVitalityIncreaseModerate();
            this.paiHigh = sample.getVitalityIncreaseHigh();
            this.paiTotal = sample.getVitalityCurrent();
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
            return 0; // not supported
        }

        @Override
        public int getTimeModerate() {
            return 0; // not supported
        }

        @Override
        public int getTimeHigh() {
            return 0; // not supported
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
