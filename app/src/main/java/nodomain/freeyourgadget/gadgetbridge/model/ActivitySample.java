package nodomain.freeyourgadget.gadgetbridge.model;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public interface ActivitySample extends TimeStamped {
//    /**
//     * Returns the provider of the data.
//     *
//     * @return who created the sample data
//     */
//    SampleProvider getProvider();

    /**
     * Returns the raw activity kind value as recorded by the SampleProvider
     */
    int getRawKind();

//    /**
//     * Returns the activity kind value as recorded by the SampleProvider
//     *
//     * @see ActivityKind
//     */
//    int getKind();

    /**
     * Returns the raw intensity value as recorded by the SampleProvider
     */
    int getRawIntensity();

//    /**
//     * Returns the normalized intensity value between 0 and 1
//     */
//    float getIntensity();

    /**
     * Returns the number of steps performed during the period of this sample
     */
    int getSteps();
}
