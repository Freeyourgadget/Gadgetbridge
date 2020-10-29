package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.device_info;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.file.FileHandle;

public class SupportedFileVersionsInfo implements DeviceInfo {
    private HashMap<Byte, Short> supportedFileVersions = new HashMap<>();

    @Override
    public void parsePayload(byte[] payload) {
        ByteBuffer buffer = ByteBuffer.wrap(payload);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        while(buffer.remaining() > 0){
            byte handle = buffer.get();
            short version = buffer.getShort();
            supportedFileVersions.put(handle, version);
        }

        supportedFileVersions.put((byte) 0x15, (short) 0x0003);
    }

    public short getSupportedFileVersion(FileHandle fileHandle){
        return getSupportedFileVersion(fileHandle.getMajorHandle());
    }

    public short getSupportedFileVersion(byte fileHandle){
        return supportedFileVersions.get(fileHandle);
    }
}
