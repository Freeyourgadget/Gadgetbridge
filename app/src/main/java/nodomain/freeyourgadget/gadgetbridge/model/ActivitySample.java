package nodomain.freeyourgadget.gadgetbridge.model;

public interface ActivitySample extends Sample {
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
}
