package nodomain.freeyourgadget.gadgetbridge.model;

public class CalendarEventSpec {
    public static final byte TYPE_UNKNOWN = 0;
    public static final byte TYPE_SUNRISE = 1;
    public static final byte TYPE_SUNSET = 2;

    public byte type;
    public long id;
    public int timestamp;
    public int durationInSeconds;
    public String title;
    public String description;
}
