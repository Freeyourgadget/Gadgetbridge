package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.http;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class GarminHttpResponse {
    private int status = 200;
    private final Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    private Callable<Void> onDataSuccessfullySentListener;

    public int getStatus() {
        return status;
    }

    public void setStatus(final int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(final byte[] body) {
        this.body = body;
    }

    public Callable<Void> getOnDataSuccessfullySentListener() {
        return onDataSuccessfullySentListener;
    }

    public void setOnDataSuccessfullySentListener(final Callable<Void> onDataSuccessfullySentListener) {
        this.onDataSuccessfullySentListener = onDataSuccessfullySentListener;
    }
}
