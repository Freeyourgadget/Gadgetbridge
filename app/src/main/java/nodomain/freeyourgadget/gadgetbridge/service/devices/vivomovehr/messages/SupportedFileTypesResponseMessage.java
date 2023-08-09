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
