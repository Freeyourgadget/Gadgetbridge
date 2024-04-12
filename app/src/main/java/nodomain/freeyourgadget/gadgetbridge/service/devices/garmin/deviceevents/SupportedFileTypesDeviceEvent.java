package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;

public class SupportedFileTypesDeviceEvent extends GBDeviceEvent {

    private final List<FileType> supportedFileTypes;

    public SupportedFileTypesDeviceEvent(List<FileType> fileTypes) {
        this.supportedFileTypes = fileTypes;
    }

    public List<FileType> getSupportedFileTypes() {
        return supportedFileTypes;
    }
}
