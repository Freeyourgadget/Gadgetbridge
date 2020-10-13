package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons;

import org.json.JSONException;
import org.json.JSONObject;

public class ButtonConfiguration {
    private String triggerEvent;
    private String action;

    public ButtonConfiguration(String triggerEvent, String action) {
        this.triggerEvent = triggerEvent;
        this.action = action;
    }

    public JSONObject toJsonObject(){
        try {
            return new JSONObject()
                    .put("button_evt", triggerEvent)
                    .put("name", action);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
