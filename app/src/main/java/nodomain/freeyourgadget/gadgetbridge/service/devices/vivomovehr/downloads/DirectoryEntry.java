package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads;

import java.util.Date;

public class DirectoryEntry {
    public final int fileIndex;
    public final int fileDataType;
    public final int fileSubType;
    public final int fileNumber;
    public final int specificFlags;
    public final int fileFlags;
    public final int fileSize;
    public final Date fileDate;

    public DirectoryEntry(int fileIndex, int fileDataType, int fileSubType, int fileNumber, int specificFlags, int fileFlags, int fileSize, Date fileDate) {
        this.fileIndex = fileIndex;
        this.fileDataType = fileDataType;
        this.fileSubType = fileSubType;
        this.fileNumber = fileNumber;
        this.specificFlags = specificFlags;
        this.fileFlags = fileFlags;
        this.fileSize = fileSize;
        this.fileDate = fileDate;
    }
}
