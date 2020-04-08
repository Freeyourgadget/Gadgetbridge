package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import nodomain.freeyourgadget.gadgetbridge.R;

public class Widget implements Serializable {
    private WidgetType widgetType;
    private int angle, distance;
    private String fontColor;

    public Widget(WidgetType type, int angle, int distance, String fontColor) {
        this.widgetType = type;
        this.angle = angle;
        this.distance = distance;
        this.fontColor = fontColor;
    }

    public int getAngle() {
        return angle;
    }

    public int getDistance() {
        return distance;
    }

    public void setAngle(int angle) {
        this.angle = angle;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @NonNull
    @Override
    public String toString() {
        return toJson().toString();
    }

    public JSONObject toJson() {
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
                                    .put("font_color", fontColor)
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return object;
    }


    public enum WidgetType {
        HEART_RATE("hrSSE", R.string.hr_widget_heart_rate),
        STEPS("stepsSSE", R.string.hr_widget_steps),
        DATE("dateSSE", R.string.hr_widget_date),
        ACTIVE_MINUTES("activeMinutesSSE", R.string.hr_widget_active_minutes),
        CALORIES("caloriesSSE", R.string.hr_widget_calories),
        BATTERY("batterySSE", R.string.hr_widget_battery),
        WEATHER("weatherSSE", R.string.hr_widget_weather),
        NOTHING(null, R.string.hr_widget_nothing);

        private String identifier;
        private int stringResource;

        WidgetType(String identifier, int stringResource) {
            this.identifier = identifier;
            this.stringResource = stringResource;
        }

        public static WidgetType fromJsonIdentifier(String jsonIdentifier){
            for(WidgetType type : values()){
                if(type.getIdentifier() != null && type.getIdentifier().equals(jsonIdentifier)) return type;
            }
            return null;
        }

        public int getStringResource() {
            return stringResource;
        }

        public String getIdentifier() {
            return this.identifier;
        }
    }
}
