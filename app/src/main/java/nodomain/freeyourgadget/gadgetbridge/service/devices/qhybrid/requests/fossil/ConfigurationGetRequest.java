package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import android.util.Log;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class ConfigurationGetRequest extends FileGetRequest {
    public ConfigurationGetRequest(short handle, FossilWatchAdapter adapter) {
        super(handle, adapter);
    }

    @Override
    void handleFileData(byte[] fileData) {
        log("config file: " + getAdapter().arrayToString(fileData));
    }
}
