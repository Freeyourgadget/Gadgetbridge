package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;

public abstract class FileLookupAndGetRequest extends FileLookupRequest {
    public FileLookupAndGetRequest(byte fileType, FossilWatchAdapter adapter) {
        super(fileType, adapter);
    }

    @Override
    public void handleFileLookup(short fileHandle){
        getAdapter().queueWrite(new FileGetRequest(getHandle(), getAdapter()) {
            @Override
            public void handleFileData(byte[] fileData) {
                FileLookupAndGetRequest.this.handleFileData(fileData);
            }
        });
    }

    abstract public void handleFileData(byte[] fileData);
}
