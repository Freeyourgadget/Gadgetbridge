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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Parser of GFDI messages embedded in COBS packets.
 * <p>
 * COBS ensures there are no embedded NUL bytes inside the packet data, and wraps the message into NUL framing bytes.
 */
// Notes: not really optimized; does a lot of (re)allocation, might use more static buffers, I guess… And code cleanup as well.
public class GfdiPacketParser {
    private static final Logger LOG = LoggerFactory.getLogger(GfdiPacketParser.class);

    private static final long BUFFER_TIMEOUT = 1500L;
    private static final byte[] EMPTY_BUFFER = new byte[0];
    private static final byte[] BUFFER_FRAMING = new byte[1];

    private byte[] buffer = EMPTY_BUFFER;
    private byte[] packet;
    private byte[] packetBuffer;
    private int bufferPos;
    private long lastUpdate;
    private boolean insidePacket;

    public void reset() {
        buffer = EMPTY_BUFFER;
        bufferPos = 0;
        insidePacket = false;
        packet = null;
        packetBuffer = EMPTY_BUFFER;
    }

    public void receivedBytes(byte[] bytes) {
        final long now = System.currentTimeMillis();
        if ((now - lastUpdate) > BUFFER_TIMEOUT) {
            reset();
        }
        lastUpdate = now;
        final int bufferSize = buffer.length;
        buffer = Arrays.copyOf(buffer, bufferSize + bytes.length);
        System.arraycopy(bytes, 0, buffer, bufferSize, bytes.length);
        parseBuffer();
    }

    public byte[] retrievePacket() {
        final byte[] resultPacket = packet;
        packet = null;
        parseBuffer();
        return resultPacket;
    }

    private void parseBuffer() {
        if (packet != null) {
            // packet is waiting, unable to parse more
            return;
        }
        if (bufferPos >= buffer.length) {
            // nothing to parse
            return;
        }
        boolean startOfPacket = !insidePacket;
        if (startOfPacket) {
            byte b;
            while (bufferPos < buffer.length && (b = buffer[bufferPos++]) != 0) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Unexpected non-zero byte while looking for framing: {}", Integer.toHexString(b));
                }
            }
            if (bufferPos >= buffer.length) {
                // nothing to parse
                return;
            }
            insidePacket = true;
        }
        boolean endedWithFullChunk = false;
        while (bufferPos < buffer.length) {
            int chunkSize = -1;
            int chunkStart = bufferPos;
            int pos = bufferPos;
            while (pos < buffer.length && ((chunkSize = (buffer[pos++] & 0xFF)) == 0) && startOfPacket) {
                // skip repeating framing bytes (?)
                bufferPos = pos;
                chunkStart = pos;
            }
            if (startOfPacket && pos >= buffer.length) {
                // incomplete framing, needs to wait for more data and try again
                buffer = BUFFER_FRAMING;
                bufferPos = 0;
                insidePacket = false;
                return;
            }
            assert chunkSize >= 0;
            if (chunkSize == 0) {
                // end of packet
                // drop the last zero
                if (endedWithFullChunk) {
                    // except when it was explicitly added (TODO: ugly, is it correct?)
                    packet = packetBuffer;
                } else {
                    packet = Arrays.copyOf(packetBuffer, packetBuffer.length - 1);
                }
                packetBuffer = EMPTY_BUFFER;
                insidePacket = false;

                if (bufferPos == buffer.length - 1) {
                    buffer = EMPTY_BUFFER;
                    bufferPos = 0;
                } else {
                    // TODO: Realloc buffer down
                    ++bufferPos;
                }
                return;
            }
            if (chunkStart + chunkSize > buffer.length) {
                // incomplete chunk, needs to wait for more data
                return;
            }

            // completed chunk
            final int packetPos = packetBuffer.length;
            final int realChunkSize = chunkSize < 255 ? chunkSize : chunkSize - 1;
            packetBuffer = Arrays.copyOf(packetBuffer, packetPos + realChunkSize);
            System.arraycopy(buffer, chunkStart + 1, packetBuffer, packetPos, chunkSize - 1);
            bufferPos = chunkStart + chunkSize;

            endedWithFullChunk = chunkSize == 255;
            startOfPacket = false;
        }
    }

    public static byte[] wrapMessageToPacket(byte[] message) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(message.length + 2 + (message.length + 253) / 254)) {
            outputStream.write(0);
            int chunkStart = 0;
            for (int i = 0; i < message.length; ++i) {
                if (message[i] == 0) {
                    chunkStart = appendChunk(message, outputStream, chunkStart, i);
                }
            }
            if (chunkStart <= message.length) {
                appendChunk(message, outputStream, chunkStart, message.length);
            }
            outputStream.write(0);
            return outputStream.toByteArray();
        } catch (IOException e) {
            LOG.error("Error writing to memory buffer", e);
            throw new RuntimeException(e);
        }
    }

    private static int appendChunk(byte[] message, ByteArrayOutputStream outputStream, int chunkStart, int messagePos) {
        int chunkLength = messagePos - chunkStart;
        while (true) {
            if (chunkLength >= 255) {
                // write 255-byte chunk
                outputStream.write(255);
                outputStream.write(message, chunkStart, 254);
                chunkLength -= 254;
                chunkStart += 254;
            } else {
                // write chunk from chunkStart to here
                outputStream.write(chunkLength + 1);
                outputStream.write(message, chunkStart, chunkLength);
                chunkStart = messagePos + 1;
                break;
            }
        }
        return chunkStart;
    }
}
