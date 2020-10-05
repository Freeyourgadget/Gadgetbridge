/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti
    Copyright (C) 2020 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public abstract class BaseCommand {
    // Common constants
    public static final byte OP_GET = 0;
    public static final byte OP_SET = 1;

    abstract protected void deserializeParams(byte id, ByteBuffer params);
    abstract protected byte serializeParams(ByteBuffer params);

    public void deserialize(byte[] response) {
        if (response.length < LefunConstants.CMD_HEADER_LENGTH || response.length < response[1])
            throw new IllegalArgumentException("Response is too short");
        if (calculateChecksum(response, 0, response[1] - 1) != response[response[1] - 1])
            throw new IllegalArgumentException("Incorrect message checksum");
        ByteBuffer buffer = ByteBuffer.wrap(response, LefunConstants.CMD_HEADER_LENGTH - 1,
                response[1] - LefunConstants.CMD_HEADER_LENGTH);
        buffer.order(ByteOrder.BIG_ENDIAN);
        deserializeParams(response[2], buffer);
    }

    public byte[] serialize() {
        ByteBuffer buffer = ByteBuffer.allocate(LefunConstants.CMD_MAX_LENGTH - LefunConstants.CMD_HEADER_LENGTH);
        buffer.order(ByteOrder.BIG_ENDIAN);
        byte id = serializeParams(buffer);
        return makeCommand(id, buffer);
    }

    public static byte calculateChecksum(byte[] data, int offset, int length) {
        int checksum = 0;
        for (int i = offset; i < offset + length; ++i) {
            byte b = data[i];
            for (int j = 0; j < 8; ++j) {
                if (((b ^ checksum) & 1) == 0) {
                    checksum >>= 1;
                } else {
                    checksum = (checksum ^ 0x18) >> 1 | 0x80;
                }
                b >>= 1;
            }
        }
        return (byte)checksum;
    }

    /**
     * Builds a command given ID and parameters buffer.
     *
     * @param id     the command ID
     * @param params the parameters buffer
     * @return the assembled command buffer
     */
    protected byte[] makeCommand(byte id, ByteBuffer params) {
        if (params.position() > LefunConstants.CMD_MAX_LENGTH - LefunConstants.CMD_HEADER_LENGTH)
            throw new IllegalArgumentException("params is too long to fit");

        int paramsLength = params.position();
        byte[] request = new byte[paramsLength + LefunConstants.CMD_HEADER_LENGTH];
        request[0] = LefunConstants.CMD_REQUEST_ID;
        request[1] = (byte) request.length;
        request[2] = id;
        params.flip();
        params.get(request, LefunConstants.CMD_HEADER_LENGTH - 1, paramsLength);
        request[request.length - 1] = calculateChecksum(request, 0, request.length - 1);
        return request;
    }

    protected void throwUnexpectedLength() {
        throw new IllegalArgumentException("Unexpected parameters length");
    }

    protected void validateId(byte id, byte expectedId) {
        if (id != expectedId)
            throw new IllegalArgumentException("Wrong command ID");
    }

    protected void validateIdAndLength(byte id, ByteBuffer params, byte expectedId, int expectedLength) {
        validateId(id, expectedId);
        if (params.limit() - params.position() != expectedLength)
            throwUnexpectedLength();
    }

    protected boolean getBit(int value, int mask) {
        return (value & mask) != 0;
    }

    protected int setBit(int value, int mask, boolean set) {
        if (set) {
            return value | mask;
        } else {
            return value & ~mask;
        }
    }

    protected short setBit(short value, int mask, boolean set) {
        if (set) {
            return (short)(value | mask);
        } else {
            return (short)(value & ~mask);
        }
    }

    protected byte setBit(byte value, int mask, boolean set) {
        if (set) {
            return (byte)(value | mask);
        } else {
            return (byte)(value & ~mask);
        }
    }

    /**
     * Find index of first bit that is set
     * @param value
     * @return
     */
    protected int getLowestSetBitIndex(int value) {
        if (value == 0) return -1;

        int i = 0;
        while ((value & 1) == 0) {
            ++i;
            value >>= 1;
        }
        return i;
    }
}
