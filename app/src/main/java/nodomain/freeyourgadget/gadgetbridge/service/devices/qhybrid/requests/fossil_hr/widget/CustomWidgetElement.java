package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

public class CustomWidgetElement {
    public enum WidgetElementType {
        TYPE_TEXT,
        TYPE_IMAGE,
        TYPE_BACKGROUND
    }

    public final static int X_CENTER = 38;
    public final static int Y_UPPER_HALF = (int) (76f / 3 * 1);
    public final static int Y_LOWER_HALF = (int) (76f / 3 * 2);

    private WidgetElementType widgetElementType;
    private String id, value;
    private int x, y;

    protected CustomWidgetElement(WidgetElementType widgetElementType, String id, String value, int x, int y) {
        this.widgetElementType = widgetElementType;
        this.id = id;
        this.value = value;
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public WidgetElementType getWidgetElementType() {
        return widgetElementType;
    }

    public String getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
