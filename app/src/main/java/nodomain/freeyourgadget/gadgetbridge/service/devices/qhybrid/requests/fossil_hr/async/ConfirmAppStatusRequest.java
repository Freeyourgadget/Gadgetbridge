package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.async;

import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class ConfirmAppStatusRequest extends JsonPutRequest {
    public ConfirmAppStatusRequest(int requestId, FossilHRWatchAdapter adapter) {
        super(createResponseObject(requestId), adapter);
    }

    private static JSONObject createResponseObject(int requestId){
        try {
            return new JSONObject()
                    .put("res",
                            new JSONObject()
                            .put("id", requestId)
                            .put("set", new JSONObject()
                                    .put("master._.config.app_status", new JSONObject()
                                            .put("message", "")
                                            .put("type", "success")
                                    )
                            )
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
