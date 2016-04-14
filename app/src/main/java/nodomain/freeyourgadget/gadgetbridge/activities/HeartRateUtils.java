package nodomain.freeyourgadget.gadgetbridge.activities;

public class HeartRateUtils {
    public static final int MAX_HEART_RATE_VALUE = 250;
    public static final int MIN_HEART_RATE_VALUE = 0;
    /**
     * The maxiumum gap between two hr measurements in which
     * we interpolate between the measurements. Otherwise, two
     * distinct measurements will be shown.
     *
     * Value is in minutes
     */
    public static final int MAX_HR_MEASUREMENTS_GAP_MINUTES = 10;
}
