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

public class FileTransferDataMessage {
    public final int flags;
    public final int crc;
    public final int dataOffset;
    public final byte[] data;

    public final byte[] packet;

    public FileTransferDataMessage(int flags, int crc, int dataOffset, byte[] data) {
        this.flags = flags;
        this.crc = crc;
        this.dataOffset = dataOffset;
        this.data = data;

        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_FILE_TRANSFER_DATA);
        writer.writeByte(flags);
        writer.writeShort(crc);
        writer.writeInt(dataOffset);
        writer.writeBytes(data);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }

    public static FileTransferDataMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);

        final int flags = reader.readByte();
        final int crc = reader.readShort();
        final int dataOffset = reader.readInt();
        final int dataSize = packet.length - 13;
        final byte[] data = reader.readBytes(dataSize);

        return new FileTransferDataMessage(flags, crc, dataOffset, data);
    }
}
