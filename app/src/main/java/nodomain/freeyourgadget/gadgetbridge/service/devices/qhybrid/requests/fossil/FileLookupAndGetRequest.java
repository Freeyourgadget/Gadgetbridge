package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public class FileLookupAndGetRequest extends FileLookupRequest {
    public FileLookupAndGetRequest(byte fileType, FossilWatchAdapter adapter) {
        super(fileType, adapter);
    }

    @Override
    public void handleFileLookup(short fileHandle) {
        this.getAdapter().queueWrite(new ConfigurationGetRequest(fileHandle, getAdapter()));
    }
}
