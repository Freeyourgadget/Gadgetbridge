package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.image;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json.JsonPutRequest;

public class ImagesSetRequest extends JsonPutRequest {
    public ImagesSetRequest(AssetImage[] images, FossilHRWatchAdapter adapter) {
        super(prepareObject(images), adapter);
    }

    private static JSONObject prepareObject(AssetImage[] images){
        try {
            JSONArray imageArray = new JSONArray();
            for (AssetImage image : images) imageArray.put(image.toJsonObject());
            return new JSONObject()
                    .put("push",
                            new JSONObject()
                            .put("set",
                                    new JSONObject()
                                    .put("watchFace._.config.backgrounds",
                                            imageArray
                                    )
                            )
                    );
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
