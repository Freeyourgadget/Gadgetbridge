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
package nodomain.freeyourgadget.gadgetbridge.devices;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.TimeSample;

/**
 * Wraps a {@link SampleProvider} into a {@link TimeSampleProvider}.
 */
public abstract class AbstractSampleToTimeSampleProvider<T extends TimeSample, S extends AbstractActivitySample> implements TimeSampleProvider<T> {
    private final SampleProvider<S> mSampleProvider;
    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractSampleToTimeSampleProvider(final SampleProvider<S> sampleProvider, final GBDevice device, final DaoSession session) {
        mSampleProvider = sampleProvider;
        mDevice = device;
        mSession = session;
    }

    @Nullable
    protected abstract T convertSample(final S sample);

    public GBDevice getDevice() {
        return mDevice;
    }

    public DaoSession getSession() {
        return mSession;
    }

    @NonNull
    @Override
    public List<T> getAllSamples(final long timestampFrom, final long timestampTo) {
        final List<S> upstreamSamples = mSampleProvider.getAllActivitySamples((int) (timestampFrom / 1000L), (int) (timestampTo / 1000L));
        final List<T> ret = new ArrayList<>();
        for (final S sample : upstreamSamples) {
            final T converted = convertSample(sample);
            if (converted != null) {
                ret.add(converted);
            }
        }
        return ret;
    }

    @Override
    public void addSample(final T timeSample) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public void addSamples(final List<T> timeSamples) {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Override
    public T createSample() {
        throw new UnsupportedOperationException("This sample provider is read-only!");
    }

    @Nullable
    @Override
    public T getLatestSample() {
        final S latestSample = mSampleProvider.getLatestActivitySample();
        return convertSample(latestSample);
    }

    @Nullable
    @Override
    public T getLatestSample(final long until) {
        final S latestSample = mSampleProvider.getLatestActivitySample((int) (until / 1000L));
        return convertSample(latestSample);
    }

    @Nullable
    @Override
    public T getFirstSample() {
        final S firstSample = mSampleProvider.getFirstActivitySample();
        return convertSample(firstSample);
    }
}
