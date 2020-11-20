package nodomain.freeyourgadget.gadgetbridge.util;

import androidx.annotation.IntRange;

public class BcdUtil {
    public static byte toBcd8(@IntRange(from = 0, to = 99) int value) {
        int high = (value / 10) << 4;
        int low = value % 10;
        return (byte) (high | low);
    }

    public static int fromBcd8(byte value) {
        int high = ((value & 0xF0) >> 4) * 10;
        int low = value & 0x0F;
        return high + low;
    }
}
