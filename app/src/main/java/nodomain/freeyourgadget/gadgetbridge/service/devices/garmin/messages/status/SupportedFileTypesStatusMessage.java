package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents.SupportedFileTypesDeviceEvent;

public class SupportedFileTypesStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final List<FileType> fileTypeInfoList;

    public SupportedFileTypesStatusMessage(GarminMessage garminMessage, Status status, List<FileType> fileTypeInfoList) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.fileTypeInfoList = fileTypeInfoList;
    }

    public static SupportedFileTypesStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());

        if (!status.equals(Status.ACK)) {
            return null;
        }
        final int typeCount = reader.readByte();
        final List<FileType> types = new ArrayList<>(typeCount);
        for (int i = 0; i < typeCount; ++i) {
            final int fileDataType = reader.readByte();
            final int fileSubType = reader.readByte();
            final String garminDeviceFileType = reader.readString();
            FileType fileType = new FileType(fileDataType, fileSubType, garminDeviceFileType);
            if (fileType.getFileType() == null) {
                LOG.warn("Watch supports a filetype that we do not support: {}/{}: {}", fileDataType, fileSubType, garminDeviceFileType);
                continue;
            }
            types.add(fileType);
        }

        return new SupportedFileTypesStatusMessage(garminMessage, status, types);
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        return Collections.singletonList(new SupportedFileTypesDeviceEvent(fileTypeInfoList));
    }
}
