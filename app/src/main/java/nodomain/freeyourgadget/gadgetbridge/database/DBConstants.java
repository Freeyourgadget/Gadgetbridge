package nodomain.freeyourgadget.gadgetbridge.database;

/**
 * TODO: Legacy, can be removed once migration support for old ActivityDatabase is removed
 */
public class DBConstants {
    public static final String DATABASE_NAME = "ActivityDatabase";

    public static final String TABLE_GBACTIVITYSAMPLES = "GBActivitySamples";
    public static final String TABLE_STEPS_PER_DAY = "StepsPerDay";

    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_INTENSITY = "intensity";
    public static final String KEY_STEPS = "steps";
    public static final String KEY_CUSTOM_SHORT = "customShort";
    public static final String KEY_TYPE = "type";
}
