package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json;

import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FilePutRawRequest;

public class JsonPutRequest extends FilePutRawRequest {
    public JsonPutRequest(short handle, JSONObject object, FossilWatchAdapter adapter) {
        super(handle, object.toString().getBytes(), adapter);
    }
}
