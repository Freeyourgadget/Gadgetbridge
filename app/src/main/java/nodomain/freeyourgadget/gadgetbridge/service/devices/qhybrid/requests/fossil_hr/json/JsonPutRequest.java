package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.json;

import org.json.JSONObject;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FilePutRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FilePutRawRequest;

public class JsonPutRequest extends FilePutRawRequest {
    public JsonPutRequest(JSONObject object, FossilHRWatchAdapter adapter) {
        super((short)(0x0500 | (adapter.getJsonIndex() & 0xFF)), object.toString().getBytes(), adapter);
    }
}
