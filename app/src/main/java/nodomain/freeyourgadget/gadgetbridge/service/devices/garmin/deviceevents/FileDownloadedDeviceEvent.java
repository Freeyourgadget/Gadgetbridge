package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.deviceevents;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileTransferHandler;

public class FileDownloadedDeviceEvent extends GBDeviceEvent {
    public FileTransferHandler.DirectoryEntry directoryEntry;
    public String localPath;
}
