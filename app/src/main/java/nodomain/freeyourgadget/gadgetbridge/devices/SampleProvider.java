/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, João Paulo Barraca

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;

/**
 * Interface to retrieve samples from the database, and also create and add samples to the database.
 * There are multiple device specific implementations, this interface defines the generic access.
 *
 * Note that the provided samples must typically be considered read-only, because they are immediately
 * removed from the session before they are returned.
 *
 * @param <T> the device/provider specific sample type (must extend AbstractActivitySample)
 */
public interface SampleProvider<T extends AbstractActivitySample> {
    // These are only used for SharedPreferences
    int PROVIDER_PEBBLE_MORPHEUZ = 1;
    int PROVIDER_PEBBLE_MISFIT = 3;
    int PROVIDER_PEBBLE_HEALTH = 4;

    int normalizeType(int rawType);

    int toRawActivityKind(int activityKind);

    float normalizeIntensity(int rawIntensity);

    /**
     * Returns the list of all samples, of any type, within the given time span.
     * @param timestamp_from the start timestamp
     * @param timestamp_to the end timestamp
     * @return the list of samples of any type
     */
    @NonNull
    List<T> getAllActivitySamples(int timestamp_from, int timestamp_to);

    /**
     * Returns the list of all samples that represent user "activity", within
     * the given time span. This excludes samples of type sleep, for example.
     * @param timestamp_from the start timestamp
     * @param timestamp_to the end timestamp
     * @return the list of samples of type user activity, e.g. non-sleep
     */
    @NonNull
    List<T> getActivitySamples(int timestamp_from, int timestamp_to);

    /**
     * Returns the list of all samples that represent "sleeping", within the
     * given time span.
     * @param timestamp_from the start timestamp
     * @param timestamp_to the end timestamp
     * @return the list of samples of type sleep
     */
    @NonNull
    List<T> getSleepSamples(int timestamp_from, int timestamp_to);

    /**
     * Adds the given sample to the database. An existing sample with the same
     * timestamp will be overwritten.
     * @param activitySample the sample to add
     */
    void addGBActivitySample(T activitySample);

    /**
     * Adds the given samples to the database. Existing samples with the same
     * timestamp will be overwritten.
     * @param activitySamples the samples to add
     */
    void addGBActivitySamples(T[] activitySamples);

    /**
     * Factory method to creates an empty sample of the correct type for this sample provider
     * @return the newly created "empty" sample
     */
    T createActivitySample();

    /**
     * Returns the activity sample with the highest timestamp. or null if none
     * @return the latest sample or null
     */
    @Nullable
    T getLatestActivitySample();
}
