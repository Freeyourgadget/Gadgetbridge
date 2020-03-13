package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

public class CustomTextWidgetElement extends CustomWidgetElement{
    public CustomTextWidgetElement(String id, String value, int x, int y) {
        super(WidgetElementType.TYPE_TEXT, id, value, x, y);
    }

    public CustomTextWidgetElement(String value, int x, int y) {
        super(WidgetElementType.TYPE_TEXT, null, value, x, y);
    }
}
