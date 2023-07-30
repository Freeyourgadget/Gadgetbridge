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

public class FileReadyMessage {
    public static final int TRIGGER_MANUAL = 0;
    public static final int TRIGGER_AUTOMATIC = 1;

    public final int fileIndex;
    public final int dataType;
    public final int fileSubtype;
    public final int fileNumber;
    public final int specificFileFlags;
    public final int generalFileFlags;
    public final int fileSize;
    public final int fileDate;
    public final int triggerMethod;

    public FileReadyMessage(int fileIndex, int dataType, int fileSubtype, int fileNumber, int specificFileFlags, int generalFileFlags, int fileSize, int fileDate, int triggerMethod) {
        this.fileIndex = fileIndex;
        this.dataType = dataType;
        this.fileSubtype = fileSubtype;
        this.fileNumber = fileNumber;
        this.specificFileFlags = specificFileFlags;
        this.generalFileFlags = generalFileFlags;
        this.fileSize = fileSize;
        this.fileDate = fileDate;
        this.triggerMethod = triggerMethod;
    }

    public static FileReadyMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);

        final int fileIndex = reader.readShort();
        final int dataType = reader.readByte();
        final int fileSubtype = reader.readByte();
        final int fileNumber = reader.readShort();
        final int specificFileFlags = reader.readByte();
        final int generalFileFlags = reader.readByte();
        final int fileSize = reader.readInt();
        final int fileDate = reader.readInt();
        final int triggerMethod = reader.readByte();

        return new FileReadyMessage(fileIndex, dataType, fileSubtype, fileNumber, specificFileFlags, generalFileFlags, fileSize, fileDate, triggerMethod);
    }
}
