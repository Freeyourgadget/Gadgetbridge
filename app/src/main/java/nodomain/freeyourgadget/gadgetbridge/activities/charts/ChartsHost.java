package nodomain.freeyourgadget.gadgetbridge.activities.charts;

public interface ChartsHost {
    public static final String DATE_PREV = ChartsActivity.class.getName().concat(".date_prev");
    public static final String DATE_NEXT = ChartsActivity.class.getName().concat(".date_next");
    public static final String REFRESH = ChartsActivity.class.getName().concat(".refresh");

    void setDateInfo(String dateInfo);
}
