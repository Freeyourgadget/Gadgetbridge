package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

public class CustomBackgroundWidgetElement extends CustomWidgetElement{
    public CustomBackgroundWidgetElement(String id, String value) {
        super(WidgetElementType.TYPE_BACKGROUND, id, value, 0, 0);
    }

    public CustomBackgroundWidgetElement(String value) {
        super(WidgetElementType.TYPE_BACKGROUND, null, value, 0, 0);
    }
}
