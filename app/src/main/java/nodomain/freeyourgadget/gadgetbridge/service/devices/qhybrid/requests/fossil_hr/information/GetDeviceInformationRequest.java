package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.information;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil.FossilWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.file.FileEncryptedLookupAndGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.utils.StringUtils;

public class GetDeviceInformationRequest extends FileEncryptedLookupAndGetRequest {
    public GetDeviceInformationRequest(FossilHRWatchAdapter adapter) {
        super((byte) 0x08, adapter);
    }

    @Override
    public void handleFileData(byte[] fileData) {
        log("device info: " + StringUtils.bytesToHex(fileData));
    }
}
