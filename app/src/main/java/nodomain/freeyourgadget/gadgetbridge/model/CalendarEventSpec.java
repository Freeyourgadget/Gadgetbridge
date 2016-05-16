package nodomain.freeyourgadget.gadgetbridge.model;

public class CalendarEventSpec {
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_SUNRISE = 1;
    public static final int TYPE_SUNSET = 2;

    public int type;
    public long id;
    public int timestamp;
    public int durationInSeconds;
    public String title;
    public String description;
}
