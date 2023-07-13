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

public class FileTransferDataResponseMessage {
    public static final byte RESPONSE_TRANSFER_SUCCESSFUL = 0;
    public static final byte RESPONSE_RESEND_LAST_DATA_PACKET = 1;
    public static final byte RESPONSE_ABORT_DOWNLOAD_REQUEST = 2;
    public static final byte RESPONSE_ERROR_CRC_MISMATCH = 3;
    public static final byte RESPONSE_ERROR_DATA_OFFSET_MISMATCH = 4;
    public static final byte RESPONSE_SILENT_SYNC_PAUSED = 5;

    public final int status;
    public final int response;
    public final int nextDataOffset;

    public final byte[] packet;

    public FileTransferDataResponseMessage(int status, int response, int nextDataOffset) {
        this.status = status;
        this.response = response;
        this.nextDataOffset = nextDataOffset;

        final MessageWriter writer = new MessageWriter();
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(VivomoveConstants.MESSAGE_RESPONSE);
        writer.writeShort(VivomoveConstants.MESSAGE_FILE_TRANSFER_DATA);
        writer.writeByte(status);
        writer.writeByte(response);
        writer.writeInt(nextDataOffset);
        writer.writeShort(0); // CRC will be filled below
        final byte[] packet = writer.getBytes();
        BLETypeConversions.writeUint16(packet, 0, packet.length);
        BLETypeConversions.writeUint16(packet, packet.length - 2, ChecksumCalculator.computeCrc(packet, 0, packet.length - 2));
        this.packet = packet;
    }

    public static FileTransferDataResponseMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 6);
        final int status = reader.readByte();
        final int response = reader.readByte();
        final int nextDataOffset = reader.readInt();

        return new FileTransferDataResponseMessage(status, response, nextDataOffset);
    }
}
