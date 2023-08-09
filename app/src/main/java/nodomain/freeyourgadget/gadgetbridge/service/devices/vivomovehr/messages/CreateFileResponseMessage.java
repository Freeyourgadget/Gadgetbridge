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

public class CreateFileResponseMessage {
    public static final byte RESPONSE_FILE_CREATED_SUCCESSFULLY = 0;
    public static final byte RESPONSE_FILE_ALREADY_EXISTS = 1;
    public static final byte RESPONSE_NOT_ENOUGH_SPACE = 2;
    public static final byte RESPONSE_NOT_SUPPORTED = 3;
    public static final byte RESPONSE_NO_SLOTS_AVAILABLE_FOR_FILE_TYPE = 4;
    public static final byte RESPONSE_NOT_ENOUGH_SPACE_FOR_FILE_TYPE = 5;

    public final int status;
    public final int response;
    public final int fileIndex;
    public final int dataType;
    public final int subType;
    public final int fileNumber;

    public CreateFileResponseMessage(int status, int response, int fileIndex, int dataType, int subType, int fileNumber) {
        this.status = status;
        this.response = response;
        this.fileIndex = fileIndex;
        this.dataType = dataType;
        this.subType = subType;
        this.fileNumber = fileNumber;
    }

    public static CreateFileResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 6);
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int fileIndex = reader.readShort();
        final int dataType = reader.readByte();
        final int subType = reader.readByte();
        final int fileNumber = reader.readShort();

        return new CreateFileResponseMessage(status, response, fileIndex, dataType, subType, fileNumber);
    }
}
