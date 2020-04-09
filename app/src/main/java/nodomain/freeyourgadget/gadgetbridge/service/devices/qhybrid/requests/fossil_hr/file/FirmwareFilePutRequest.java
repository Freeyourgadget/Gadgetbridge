package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;

public class FirmwareFilePutRequest extends FilePutRawRequest {
    public FirmwareFilePutRequest(byte[] firmwareBytes, FossilHRWatchAdapter adapter) {
        super((short) 0x00FF, firmwareBytes, adapter);
    }
}
