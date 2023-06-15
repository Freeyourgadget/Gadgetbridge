package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

import java.util.ArrayList;
import java.util.List;

public class SupportedFileTypesResponseMessage {
    public static final int FILE_DATA_TYPE_FIT = 128;
    public static final int FILE_DATA_TYPE_GRAPHIC = 2;
    public static final int FILE_DATA_TYPE_INVALID = -1;
    public static final int FILE_DATA_TYPE_NON_FIT = 255;

    public final int status;
    public final List<FileTypeInfo> fileTypes;

    public SupportedFileTypesResponseMessage(int status, List<FileTypeInfo> fileTypes) {
        this.status = status;
        this.fileTypes = fileTypes;
    }

    public static SupportedFileTypesResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int status = reader.readByte();

        final int typeCount = reader.readByte();
        final List<FileTypeInfo> types = new ArrayList<>(typeCount);
        for (int i = 0; i < typeCount; ++i) {
            final int fileDataType = reader.readByte();
            final int fileSubType = reader.readByte();
            final String garminDeviceFileType = reader.readString();
            types.add(new FileTypeInfo(fileDataType, fileSubType, garminDeviceFileType));
        }

        return new SupportedFileTypesResponseMessage(status, types);
    }

    public static class FileTypeInfo {
        public final int fileDataType;
        public final int fileSubType;
        public final String garminDeviceFileType;

        public FileTypeInfo(int fileDataType, int fileSubType, String garminDeviceFileType) {
            this.fileDataType = fileDataType;
            this.fileSubType = fileSubType;
            this.garminDeviceFileType = garminDeviceFileType;
        }
    }
}
