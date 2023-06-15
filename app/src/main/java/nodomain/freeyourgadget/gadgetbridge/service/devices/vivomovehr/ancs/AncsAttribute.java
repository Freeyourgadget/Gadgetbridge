package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import android.util.SparseArray;

public enum AncsAttribute {
    APP_IDENTIFIER(0),
    TITLE(1, true),
    SUBTITLE(2, true),
    MESSAGE(3, true),
    MESSAGE_SIZE(4),
    DATE(5),
    POSITIVE_ACTION_LABEL(6),
    NEGATIVE_ACTION_LABEL(7),
    // Garmin extensions
    PHONE_NUMBER(126, true),
    ACTIONS(127, false, true);

    private static final SparseArray<AncsAttribute> valueByCode;

    public final int code;
    public final boolean hasLengthParam;
    public final boolean hasAdditionalParams;

    AncsAttribute(int code) {
        this(code, false, false);
    }

    AncsAttribute(int code, boolean hasLengthParam) {
        this(code, hasLengthParam, false);
    }

    AncsAttribute(int code, boolean hasLengthParam, boolean hasAdditionalParams) {
        this.code = code;
        this.hasLengthParam = hasLengthParam;
        this.hasAdditionalParams = hasAdditionalParams;
    }

    static {
        final AncsAttribute[] values = values();
        valueByCode = new SparseArray<>(values.length);
        for (AncsAttribute value : values) {
            valueByCode.append(value.code, value);
        }
    }

    public static AncsAttribute getByCode(int code) {
        return valueByCode.get(code);
    }
}
