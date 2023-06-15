package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import android.util.SparseArray;

public enum AncsCommand {
    GET_NOTIFICATION_ATTRIBUTES(0),
    GET_APP_ATTRIBUTES(1),
    PERFORM_NOTIFICATION_ACTION(2),
    // Garmin extensions
    PERFORM_ANDROID_ACTION(128);

    private static final SparseArray<AncsCommand> valueByCode;

    public final int code;

    AncsCommand(int code) {
        this.code = code;
    }

    static {
        final AncsCommand[] values = values();
        valueByCode = new SparseArray<>(values.length);
        for (AncsCommand value : values) {
            valueByCode.append(value.code, value);
        }
    }

    public static AncsCommand getByCode(int code) {
        return valueByCode.get(code);
    }
}
