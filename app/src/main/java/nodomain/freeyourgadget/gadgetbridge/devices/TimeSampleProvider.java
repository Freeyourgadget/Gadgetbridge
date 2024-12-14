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

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.model.TimeSample;

/**
 * Interface to retrieve samples from the database, and also create and add samples to the database.
 * There are multiple device specific implementations, this interface defines the generic access.
 * <p>
 * Note that the provided samples must typically be considered read-only, because they are immediately
 * removed from the session before they are returned.
 * <p>
 * This differs from SampleProvider by assuming milliseconds for timestamps instead of seconds, as well as
 * not enforcing an all-in-once ActivitySample interface for data that might be completely unrelated with
 * activity data.
 *
 * @param <T> the device/provider specific sample type (must extend TimeSample).
 */
public interface TimeSampleProvider<T extends TimeSample> {
    /**
     * Returns the list of all samples, of any type, within the given time span.
     *
     * @param timestampFrom the start timestamp, in milliseconds
     * @param timestampTo   the end timestamp, in milliseconds
     * @return the list of samples of any type
     */
    @NonNull
    List<T> getAllSamples(long timestampFrom, long timestampTo);

    /**
     * Adds the given sample to the database. An existing sample with the same
     * timestamp will be overwritten.
     *
     * @param timeSample the sample to add
     */
    void addSample(T timeSample);

    /**
     * Adds the given samples to the database. Existing samples with the same
     * timestamp (and a potential combination of other attributes, depending on the sample implementation)
     * will be overwritten.
     *
     * @param timeSamples the samples to add
     */
    void addSamples(List<T> timeSamples);

    /**
     * Factory method to creates an empty sample of the correct type for this sample provider.
     *
     * @return the newly created "empty" sample
     */
    T createSample();

    /**
     * Returns the sample with the highest timestamp, or null if none.
     *
     * @return the latest sample, or null if none is found.
     */
    @Nullable
    T getLatestSample();

    /**
     * Returns the sample with the highest timestamp until a limit, or null if none.
     *
     * @param until maximum timestamp of the sample, inclusive
     *
     * @return the latest sample, or null if none is found.
     */
    @Nullable
    T getLatestSample(long until);

    /**
     * Returns the sample with the oldest timestamp, or null if none.
     *
     * @return the oldest sample, or null if none is found
     */
    @Nullable
    T getFirstSample();
}
