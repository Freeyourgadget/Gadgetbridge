package nodomain.freeyourgadget.gadgetbridge.charts;

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.model.SampleProvider;

public class ActivityKind {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_ACTIVITY = 1;
    public static final int TYPE_LIGHT_SLEEP = 2;
    public static final int TYPE_DEEP_SLEEP = 4;
    public static final int TYPE_SLEEP = TYPE_LIGHT_SLEEP | TYPE_DEEP_SLEEP;
    public static final int TYPE_ALL = TYPE_ACTIVITY | TYPE_SLEEP;

    public static byte[] mapToDBActivityTypes(int types, SampleProvider provider) {
        byte[] result = new byte[3];
        int i = 0;
        if ((types & ActivityKind.TYPE_ACTIVITY) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_ACTIVITY);
        }
        if ((types & ActivityKind.TYPE_DEEP_SLEEP) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_DEEP_SLEEP);
        }
        if ((types & ActivityKind.TYPE_LIGHT_SLEEP) != 0) {
            result[i++] = provider.toRawActivityKind(TYPE_LIGHT_SLEEP);
        }
        return Arrays.copyOf(result, i);
    }

}
