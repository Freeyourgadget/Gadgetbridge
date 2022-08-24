package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.alexa;

import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class AlexaMessageSetRequest extends JsonPutRequest {
    public AlexaMessageSetRequest(String message, boolean isResponse, FossilHRWatchAdapter adapter) {
        super(createResponseObject(message, isResponse), adapter);
    }

    public static JSONObject createResponseObject(String message, boolean isResponse){
        try {
            return new JSONObject()
                    .put("push", new JSONObject()
                            .put("set", new JSONObject()
                                    .put("AlexaApp._.config.msg", new JSONObject()
                                            .put("res", message)
                                            .put("is_resp", isResponse)
                                    )
                            )
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
