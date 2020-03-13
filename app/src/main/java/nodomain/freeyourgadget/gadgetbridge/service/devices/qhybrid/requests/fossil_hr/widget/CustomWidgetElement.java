package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.R;

public class CustomWidgetElement implements Serializable {
    public enum WidgetElementType {
        TYPE_TEXT(R.id.qhybrid_widget_elements_type_text, "text"),
        TYPE_IMAGE(0, "image"),
        TYPE_BACKGROUND(R.id.qhybrid_widget_elements_type_background, "background");

        private String jsonIdentifier;
        private int radioButtonResource;

        WidgetElementType(int radioButtonResource, String jsonIdentifier){
            this.radioButtonResource = radioButtonResource;
            this.jsonIdentifier = jsonIdentifier;
        }

        public int getRadioButtonResource() {
            return radioButtonResource;
        }

        public String getJsonIdentifier() {
            return jsonIdentifier;
        }

        static public WidgetElementType fromRadioButtonRessource(int radioButtonResource){
            for(WidgetElementType type : values()){
                if(type.getRadioButtonResource() == radioButtonResource) return type;
            }
            return null;
        }
    }

    public final static int X_CENTER = 38;
    public final static int Y_UPPER_HALF = (int) (76f / 3 * 1);
    public final static int Y_LOWER_HALF = (int) (76f / 3 * 2);

    private WidgetElementType widgetElementType;
    private String id, value;
    private int x, y;

    public void setWidgetElementType(WidgetElementType widgetElementType) {
        this.widgetElementType = widgetElementType;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

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
