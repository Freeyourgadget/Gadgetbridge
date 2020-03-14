package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.buttons;

import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ButtonConfigurationPutRequest extends JsonPutRequest {
    public ButtonConfigurationPutRequest(String[] menuItems, String upperButtonApp, String middleButtonApp, String lowerButtonApp, FossilHRWatchAdapter adapter) {
        super(createObject(menuItems, upperButtonApp, middleButtonApp, lowerButtonApp), adapter);
    }

    private static JSONObject createObject(String[] menuItems, String upperButtonApp, String middleButtonApp, String lowerButtonApp) {
        try {
            return new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("commuteApp._.config.destinations", new JSONArray(menuItems))
                                    .put("master._.config.buttons", new JSONArray()
                                            .put(new JSONObject()
                                                    .put("name", upperButtonApp)
                                                    .put("button_evt", "top_short_press_release")
                                            )
                                            .put(new JSONObject()
                                                    .put("name", middleButtonApp)
                                                    .put("button_evt", "middle_short_press_release")
                                            )
                                            .put(new JSONObject()
                                                    .put("name", lowerButtonApp)
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
