package nodomain.freeyourgadget.gadgetbridge.model;

public interface HeartRateSample extends TimeStamped {
    /**
     * Returns the heart rate measured at the corresponding timestamp.
     * The value is returned in heart beats per minute, in the range from
     * 0-255, where 255 is an illegal value (e.g. due to a bad measurement)
     * @return the heart rate value in beats per minute, or null if none
     */
    Integer getHeartRate();
}
