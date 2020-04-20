package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.activity;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedGetRequest;

public class ActivityFilesGetRequest extends FileEncryptedGetRequest {
    public ActivityFilesGetRequest(FossilHRWatchAdapter adapter) {
        super((short) 0x0101, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        assert Boolean.TRUE;
    }
}
