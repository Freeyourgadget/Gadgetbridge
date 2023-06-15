package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import android.util.SparseArray;

public enum AncsAndroidAction {
    REPLY_TEXT_MESSAGE(94),
    REPLY_INCOMING_CALL(95),
    ACCEPT_INCOMING_CALL(96),
    REJECT_INCOMING_CALL(97),
    DISMISS_NOTIFICATION(98),
    BLOCK_APPLICATION(99);

    private static final SparseArray<AncsAndroidAction> valueByCode;

    public final int code;

    AncsAndroidAction(int code) {
        this.code = code;
    }

    static {
        final AncsAndroidAction[] values = values();
        valueByCode = new SparseArray<>(values.length);
        for (AncsAndroidAction value : values) {
            valueByCode.append(value.code, value);
        }
    }

    public static AncsAndroidAction getByCode(int code) {
        return valueByCode.get(code);
    }
}
