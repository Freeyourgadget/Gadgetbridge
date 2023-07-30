/*  Copyright (C) 2020-2023 Petr Kadlec

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
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
