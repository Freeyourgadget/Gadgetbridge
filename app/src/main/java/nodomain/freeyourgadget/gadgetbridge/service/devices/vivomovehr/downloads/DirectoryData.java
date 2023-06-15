package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.downloads;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.GarminTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageReader;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DirectoryData {
    public final List<DirectoryEntry> entries;

    public DirectoryData(List<DirectoryEntry> entries) {
        this.entries = entries;
    }

    public static DirectoryData parse(byte[] bytes) {
        int size = bytes.length;
        if ((size % 16) != 0) throw new IllegalArgumentException("Invalid directory data length");
        int count = (size - 16) / 16;
        final MessageReader reader = new MessageReader(bytes, 16);
        final List<DirectoryEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; ++i) {
            final int fileIndex = reader.readShort();
            final int fileDataType = reader.readByte();
            final int fileSubType = reader.readByte();
            final int fileNumber = reader.readShort();
            final int specificFlags = reader.readByte();
            final int fileFlags = reader.readByte();
            final int fileSize = reader.readInt();
            final Date fileDate = new Date(GarminTimeUtils.garminTimestampToJavaMillis(reader.readInt()));

            entries.add(new DirectoryEntry(fileIndex, fileDataType, fileSubType, fileNumber, specificFlags, fileFlags, fileSize, fileDate));
        }

        return new DirectoryData(entries);
    }
}
