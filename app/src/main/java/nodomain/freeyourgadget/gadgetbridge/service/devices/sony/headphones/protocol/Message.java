/*  Copyright (C) 2021-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.headphones.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class Message {
    private static final Logger LOG = LoggerFactory.getLogger(Message.class);

    /**
     * Message format:
     * <p>
     * - MESSAGE_HEADER
     * - Message Type ({@link MessageType})
     * - Sequence Number - needs to be updated with the one sent in the ACK responses
     * - Payload Length - 4-byte big endian int with number of bytes that will follow
     * - N bytes of payload data (first being the PayloadType)
     * - Checksum (1-byte sum, excluding header)
     * - MESSAGE_TRAILER
     * <p>
     * Data between MESSAGE_HEADER and MESSAGE_TRAILER is escaped with MESSAGE_ESCAPE, and the
     * following byte masked with MESSAGE_ESCAPE_MASK.
     */

    public static final byte MESSAGE_HEADER = 0x3e;
    public static final byte MESSAGE_TRAILER = 0x3c;
    public static final byte MESSAGE_ESCAPE = 0x3d;
    public static final byte MESSAGE_ESCAPE_MASK = (byte) 0b11101111;

    private final MessageType type;
    private final byte sequenceNumber;
    private final byte[] payload;

    public Message(final MessageType type, final byte sequenceNumber, final byte[] payload) {
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.payload = payload;
    }

    public byte[] encode() {
        final ByteBuffer buf = ByteBuffer.allocate(payload.length + 6);
        buf.order(ByteOrder.BIG_ENDIAN);

        buf.put(type.getCode());
        buf.put(sequenceNumber);
        buf.putInt(payload.length);
        buf.put(payload);

        return encodeMessage(buf.array());
    }

    public MessageType getType() {
        return this.type;
    }

    public byte getSequenceNumber() {
        return this.sequenceNumber;
    }

    public byte[] getPayload() {
        return this.payload;
    }

    public String toString() {
        if (payload.length > 0) {
            return String.format(Locale.getDefault(), "Message{Cmd=%s, Seq=%d, PayloadType=%d, Payload=%s}", type, sequenceNumber, payload[0], GB.hexdump(payload));
        } else {
            return String.format(Locale.getDefault(), "Message{Cmd=%s, Seq=%d}", type, sequenceNumber);
        }
    }

    public static Message fromBytes(final byte[] rawBytes) {
        if (rawBytes[0] != MESSAGE_HEADER) {
            throw new IllegalArgumentException(String.format("Invalid header %02x", rawBytes[0]));
        }

        if (rawBytes[rawBytes.length - 1] != MESSAGE_TRAILER) {
            throw new IllegalArgumentException(String.format("Invalid trailer %02x", rawBytes[0]));
        }

        final byte[] messageBytes = unescape(rawBytes);
        final String hexdump = GB.hexdump(messageBytes, 0, messageBytes.length);

        final byte messageChecksum = messageBytes[messageBytes.length - 2];
        final byte expectedChecksum = calcChecksum(messageBytes, 1, messageBytes.length - 2);
        if (messageChecksum != expectedChecksum) {
            LOG.warn(String.format("Invalid checksum %02x for %s (expected %02x)", messageChecksum, hexdump, expectedChecksum));
            return null;
        }

        final int payloadLength = ((messageBytes[3] << 24) & 0xFF000000) |
                ((messageBytes[4] << 16) & 0xFF0000) |
                ((messageBytes[5] << 8) & 0xFF00) |
                (messageBytes[6] & 0xFF);
        if (payloadLength != messageBytes.length - 9) {
            LOG.warn("Unexpected payload length {}, expected {}", messageBytes.length - 7, payloadLength);
            return null;
        }

        final byte rawMessageType = messageBytes[1];
        final MessageType messageType = MessageType.fromCode(rawMessageType);
        if (messageType == null) {
            LOG.warn("Unknown message type {}", String.format("%02x", rawMessageType));
            return null;
        }

        final byte sequenceNumber = messageBytes[2];
        final byte[] payload = new byte[payloadLength];
        System.arraycopy(messageBytes, 7, payload, 0, payloadLength);

        return new Message(messageType, sequenceNumber, payload);
    }

    public static byte[] encodeMessage(byte[] message) {
        final ByteArrayOutputStream cmdStream = new ByteArrayOutputStream(message.length + 2);

        cmdStream.write(MESSAGE_HEADER);

        final byte checksum = calcChecksum(message, 0, message.length);

        try {
            cmdStream.write(escape(message));
            cmdStream.write(escape(new byte[]{checksum}));
        } catch (final IOException e) {
            LOG.error("This should never happen", e);
        }

        cmdStream.write(MESSAGE_TRAILER);

        return cmdStream.toByteArray();
    }

    public static byte[] escape(byte[] bytes) {
        final ByteArrayOutputStream escapedStream = new ByteArrayOutputStream(bytes.length);

        for (byte b : bytes) {
            switch (b) {
                case MESSAGE_HEADER:
                case MESSAGE_TRAILER:
                case MESSAGE_ESCAPE:
                    escapedStream.write(MESSAGE_ESCAPE);
                    escapedStream.write(b & MESSAGE_ESCAPE_MASK);
                    break;
                default:
                    escapedStream.write(b);
                    break;
            }
        }

        return escapedStream.toByteArray();
    }

    public static byte[] unescape(byte[] bytes) {
        final ByteArrayOutputStream unescapedStream = new ByteArrayOutputStream(bytes.length);

        for (int i = 0; i < bytes.length; i++) {
            final byte b = bytes[i];
            if (b == MESSAGE_ESCAPE) {
                if (++i >= bytes.length) {
                    throw new IllegalArgumentException("Invalid escape character at end of array");
                }
                unescapedStream.write(bytes[i] | ~MESSAGE_ESCAPE_MASK);
            } else {
                unescapedStream.write(b);
            }
        }

        return unescapedStream.toByteArray();
    }

    public static byte calcChecksum(byte[] message, int start, int end) {
        int chk = 0;
        for (int i = start; i < end; i++) {
            chk += message[i] & 255;
        }
        return (byte) chk;
    }

    public static byte[] hexToBytes(final String payloadHex) {
        final String[] parts = payloadHex.split(":");

        final ByteArrayOutputStream stream = new ByteArrayOutputStream();

        for (final String b : parts) {
            stream.write((byte) ((Character.digit(b.charAt(0), 16) << 4) + Character.digit(b.charAt(1), 16)));
        }

        return stream.toByteArray();
    }
}
