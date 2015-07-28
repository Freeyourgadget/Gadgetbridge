package nodomain.freeyourgadget.gadgetbridge.charts;

public class SleepUtils {
    public static final float Y_VALUE_DEEP_SLEEP = 0.01f;
    public static final float Y_VALUE_LIGHT_SLEEP = 0.016f;

    public static final boolean isSleep(byte type) {
        return type == ActivityKind.TYPE_DEEP_SLEEP || type == ActivityKind.TYPE_LIGHT_SLEEP;
    }
}
