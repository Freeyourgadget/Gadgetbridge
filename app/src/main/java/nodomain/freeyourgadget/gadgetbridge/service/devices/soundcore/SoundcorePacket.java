/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.soundcore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class SoundcorePacket {
    private static final Logger LOG = LoggerFactory.getLogger(SoundcorePacket.class);

    private static final int HEADER_LENGTH = 10;
    private static final short START_OF_PACKET_HOST = (short)0xee08;
    private static final short START_OF_PACKET_DEVICE = (short)0xff09;
    private static final byte DIRECTION_HOST = (byte)0x00;
    private static final byte DIRECTION_DEVICE = (byte)0x01;

    private short command;
    private byte[] payload;

    public SoundcorePacket(short command) {
        this(command, new byte[] {});
    }

    public SoundcorePacket(short command, byte[] payload) {
        LOG.debug("Packet: command={}", String.format("0x%04x", command));

        this.command = command;
        this.payload = payload;
    }

    public short getCommand() {
        return command;
    }

    public byte[] getPayload() {
        return payload;
    }

    public static SoundcorePacket decode(ByteBuffer buf) {
        if (buf.remaining() < HEADER_LENGTH)
            return null;

        buf.order(ByteOrder.LITTLE_ENDIAN);

        if (buf.getShort() != START_OF_PACKET_DEVICE) {
            LOG.error("Invalid start of packet: {}", hexdump(buf.array()));
            return null;
        }

        // Skip two zero bytes
        buf.getShort();

        if (buf.get() != DIRECTION_DEVICE) {
            LOG.error("Invalid direction: {}", hexdump(buf.array()));
            return null;
        }

        short command = buf.getShort();
        short length = buf.getShort();

        if (length < HEADER_LENGTH) {
            LOG.error("Invalid length: {}", hexdump(buf.array()));
            return null;
        }

        // Skip checksum byte at the end
        byte[] payload = new byte[length - HEADER_LENGTH];
        buf.get(payload);

        return new SoundcorePacket(command, payload);
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + payload.length);

        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putShort(START_OF_PACKET_HOST);
        buf.putShort((short)0x0000);
        buf.put(DIRECTION_HOST);
        buf.putShort(command);
        buf.putShort((short)(HEADER_LENGTH + payload.length));
        buf.put(payload);

        byte checksum = 0;

        for (byte val : buf.array())
            checksum += val;

        buf.put(checksum);

        return buf.array();
    }
}
