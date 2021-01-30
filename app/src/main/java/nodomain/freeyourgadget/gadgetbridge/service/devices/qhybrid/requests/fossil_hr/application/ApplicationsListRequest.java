package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil_hr.application;

import android.content.pm.ApplicationInfo;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.adapter.fossil_hr.FossilHRWatchAdapter;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupAndGetRequest;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;
import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.file.FileLookupRequest.FILE_LOOKUP_ERROR;

public class ApplicationsListRequest extends FileLookupAndGetRequest{
    public ApplicationsListRequest(FossilHRWatchAdapter adapter) {
        super(FileHandle.APP_CODE, adapter);
    }

    public void handleFileData(byte[] fileData){
        ArrayList<ApplicationInformation> applicationInfos = new ArrayList<>();
        ByteBuffer buffer = ByteBuffer.wrap(fileData);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(12);
        while(buffer.remaining() > 4){
            short packetLength = buffer.getShort();
            buffer.get();
            int nameLength = buffer.get() - 1; // cutting off null byte
            byte[] nameBuffer = new byte[nameLength];
            buffer.get(nameBuffer);
            buffer.get(); // null byte
            byte handle = buffer.get();
            int hash = buffer.getInt();
            String version = String.format(
                    "%d.%d.%d.%d",
                    buffer.get(), buffer.get(), buffer.get(), buffer.get()
                    );
            applicationInfos.add(new ApplicationInformation(
                    new String(nameBuffer),
                    version,
                    hash,
                    handle
            ));
        }
        ((FossilHRWatchAdapter) getAdapter()).setInstalledApplications(applicationInfos);
    }
    public void handleFileLookupError(FILE_LOOKUP_ERROR error){

    }
}
