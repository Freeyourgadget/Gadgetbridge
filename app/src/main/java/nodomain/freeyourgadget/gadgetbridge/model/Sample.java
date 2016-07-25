package nodomain.freeyourgadget.gadgetbridge.model;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;

public interface Sample extends TimeStamped {
    /**
     * Returns the provider of the data.
     *
     * @return who created the sample data
     */
    SampleProvider getProvider();
}
