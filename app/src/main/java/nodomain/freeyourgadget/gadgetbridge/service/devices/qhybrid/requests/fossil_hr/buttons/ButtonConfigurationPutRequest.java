package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ButtonConfigurationPutRequest extends JsonPutRequest {
    public ButtonConfigurationPutRequest(FossilWatchAdapter adapter) {
        super((short) 0x0500, createObject(), adapter);
    }

    private static JSONObject createObject() {
        try {
            return new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("commuteApp._.config.destinations", new JSONArray()
                                            .put("LAMP 1")
                                            .put("LAMP 3")
                                            .put("LAMP 4")
                                            .put("LAMP 5")
                                            .put("LAMP 6")
                                            .put("LAMP 7")
                                            .put("LAMP 8")
                                            .put("LAMP 9")
                                            .put("LAMP 10")
                                            .put("LAMP 11")
                                            .put("LAMP 12")
                                            .put("LAMP 13")
                                            .put("LAMP 14")
                                            .put("LAMP 8")
                                            .put("LAMP 9")
                                            .put("LAMP 10")
                                            .put("LAMP 11")
                                            .put("LAMP 12")
                                            .put("LAMP 13")
                                            .put("LAMP 14")
                                    )
                                    .put("master._.config.buttons", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("name", "commuteApp")
                                                    .put("button_evt", "top_short_press_release")
                                            )
                                            .put(new JSONObject()
                                                    .put("name", "commuteApp")
                                                    .put("button_evt", "middle_short_press_release")
                                            )
                                            .put(new JSONObject()
                                                    .put("name", "commuteApp")
                                                    .put("button_evt", "bottom_short_press_release")
                                            )
                                    )
                            )
                    );
        } catch (JSONException e) {
            GB.toast("error creating json", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        return null;
    }
}
