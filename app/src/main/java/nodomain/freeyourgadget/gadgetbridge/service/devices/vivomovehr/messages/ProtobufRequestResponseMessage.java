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

public class ProtobufRequestResponseMessage {
    public static final int NO_ERROR = 0;
    public static final int UNKNOWN_REQUEST_ID = 100;
    public static final int DUPLICATE_PACKET = 101;
    public static final int MISSING_PACKET = 102;
    public static final int EXCEEDED_TOTAL_PROTOBUF_LENGTH = 103;
    public static final int PROTOBUF_PARSE_ERROR = 200;
    public static final int UNKNOWN_PROTOBUF_MESSAGE = 201;

    public final int status;
    public final int requestId;
    public final int dataOffset;
    public final int protobufStatus;
    public final int error;

    public ProtobufRequestResponseMessage(int status, int requestId, int dataOffset, int protobufStatus, int error) {
        this.status = status;
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.protobufStatus = protobufStatus;
        this.error = error;
    }

    public static ProtobufRequestResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestMessageID = reader.readShort();
        final int status = reader.readByte();
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final int protobufStatus = reader.readByte();
        final int error = reader.readByte();

        return new ProtobufRequestResponseMessage(status, requestID, dataOffset, protobufStatus, error);
    }
}
