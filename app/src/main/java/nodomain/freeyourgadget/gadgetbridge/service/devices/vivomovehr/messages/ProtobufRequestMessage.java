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

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ChecksumCalculator;

public class ProtobufRequestMessage {
    public final byte[] packet;
    public final int requestId;
    public final int dataOffset;
    public final int totalProtobufLength;
    public final int protobufDataLength;
    public final byte[] messageBytes;

    public ProtobufRequestMessage(int requestId, int dataOffset, int totalProtobufLength, int protobufDataLength, byte[] messageBytes) {
        this.requestId = requestId;
        this.dataOffset = dataOffset;
        this.totalProtobufLength = totalProtobufLength;
        this.protobufDataLength = protobufDataLength;
        this.messageBytes = messageBytes;

        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_PROTOBUF_REQUEST);
        writer.writeShort(requestId);
        writer.writeInt(dataOffset);
        writer.writeInt(totalProtobufLength);
        writer.writeInt(protobufDataLength);
        writer.writeBytes(messageBytes);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }

    public static ProtobufRequestMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);
        final int requestID = reader.readShort();
        final int dataOffset = reader.readInt();
        final int totalProtobufLength= reader.readInt();
        final int protobufDataLength = reader.readInt();
        final byte[] messageBytes = reader.readBytes(protobufDataLength);
        return new ProtobufRequestMessage(requestID, dataOffset, totalProtobufLength, protobufDataLength, messageBytes);
    }
}
