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
    public ButtonConfigurationPutRequest(String[] menuItems, ButtonConfiguration[] buttonConfigurations, FossilHRWatchAdapter adapter) {
        super(createObject(menuItems, buttonConfigurations), adapter);
    }

    private static JSONObject createObject(String[] menuItems, ButtonConfiguration[] buttonConfigurations) {
        try {
            JSONArray configuration = new JSONArray();
            for(ButtonConfiguration buttonConfiguration : buttonConfigurations){
                configuration.put(buttonConfiguration.toJsonObject());
            }
            return new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("commuteApp._.config.destinations", new JSONArray(menuItems))
                                    .put("master._.config.buttons", configuration)
                            )
                    );
        } catch (JSONException e) {
            GB.toast("error creating json", Toast.LENGTH_LONG, GB.ERROR, e);
        }

        return null;
    }
}
