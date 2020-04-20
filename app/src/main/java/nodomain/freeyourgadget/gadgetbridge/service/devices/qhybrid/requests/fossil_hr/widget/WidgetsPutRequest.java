package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.widget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class WidgetsPutRequest extends JsonPutRequest {
    public WidgetsPutRequest(Widget[] widgets, FossilHRWatchAdapter adapter) {
        super(prepareFile(widgets), adapter);
    }

    private static JSONObject prepareFile(Widget[] widgets){
        try {
            JSONArray widgetArray = new JSONArray();

            for(Widget widget : widgets){
                widgetArray.put(widget.toJson());
            }

            JSONObject object = new JSONObject()
                    .put(
                            "push",
                            new JSONObject()
                            .put("set",
                                new JSONObject().put(
                                        "watchFace._.config.comps", widgetArray
                                )
                            )
                    );
            return object;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
