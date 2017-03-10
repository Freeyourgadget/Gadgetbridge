/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.model;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

/**
 * The all-in-one interface for a sample measured by a device with
 * one or more sensors.
 *
 * Each sample is the result of one or more measurements. The values are set for
 * a specific point in time (see @{link #getTimestamp()}).
 *
 * If the sample relates to a user activity (e.g. sleeping, walking, running, ...)
 * then the activity is provided through @{link #getKind()}.
 *
 * Methods will return @{link #NOT_MEASURED} in case no value is available for this
 * sample.
 *
 * The frequency of samples, i.e. the how many samples are recorded per minute, is not specified
 * and may vary.
 */
public interface ActivitySample extends TimeStamped {

    int NOT_MEASURED = -1;

    /**
     * Returns the provider of the data.
     *
     * @return who created the sample data
     */
    SampleProvider getProvider();

    /**
     * Returns the raw activity kind value as recorded by the SampleProvider
     */
    int getRawKind();

    /**
     * Returns the activity kind value as recorded by the SampleProvider
     *
     * @see ActivityKind
     */
    int getKind();

    /**
     * Returns the raw intensity value as recorded by the SampleProvider
     */
    int getRawIntensity();

    /**
     * Returns the normalized intensity value between 0 and 1
     */
    float getIntensity();

    /**
     * Returns the number of steps performed during the period of this sample
     */
    int getSteps();

    /**
     * Returns the heart rate measured at the corresponding timestamp.
     * The value is returned in heart beats per minute, in the range from
     * 0-255, where 255 is an illegal value (e.g. due to a bad measurement)
     *
     * @return the heart rate value in beats per minute, or -1 if none
     */
    int getHeartRate();

    /**
     * Sets the heart rate value of this sample. Typically only used in
     * generic db migration.
     *
     * @param value the value in bpm
     */
    void setHeartRate(int value);
}
