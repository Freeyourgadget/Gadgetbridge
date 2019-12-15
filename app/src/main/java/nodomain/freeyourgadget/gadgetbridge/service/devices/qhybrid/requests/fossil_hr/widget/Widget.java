package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Widget {
    private WidgetType widgetType;
    int angle, distance;

    public Widget(WidgetType type, int angle, int distance){
        this.widgetType = type;
    }

    @NonNull
    @Override
    public String toString() {
        return toJson().toString();
    }

    public JSONObject toJson(){
        JSONObject object = new JSONObject();

        try {
            object
                    .put("name", widgetType.getIdentifier())
                    .put("pos",
                            new JSONObject()
                            .put("angle", angle)
                            .put("distance", distance)
                            )
                    .put("data", new JSONObject())
                    .put("theme",
                            new JSONObject()
                            .put("font_color", "default")
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }


    enum WidgetType{
        TIMEZONE("timeZone2SSE");

        private String identifier;


        WidgetType(String identifier){
            this.identifier = identifier;
        }

        public String getIdentifier(){
            return this.identifier;
        }
    }
}
