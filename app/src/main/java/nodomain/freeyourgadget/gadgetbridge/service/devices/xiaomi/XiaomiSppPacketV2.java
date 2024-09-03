/*  Copyright (C) 2024 Yoran Vulker

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.util.GB;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiChannelHandler.Channel;

public abstract class XiaomiSppPacketV2 {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSppPacketV2.class);

    public static final byte[] PACKET_PREAMBLE = new byte[]{(byte) 0xa5, (byte) 0xa5};

    // TODO NACK
    public static final int PACKET_TYPE_UNKNOWN = -1;
    public static final int PACKET_TYPE_ACK = 1;
    public static final int PACKET_TYPE_SESSION_CONFIG = 2;
    public static final int PACKET_TYPE_DATA = 3;

    private final int sequenceNumber;
    private final int packetType;

    protected abstract byte[] getPacketPayloadBytes(XiaomiAuthService authService);

    public static abstract class Builder<T extends Builder<T>> {
        int packetNumber = -1;
        int packetType = PACKET_TYPE_UNKNOWN;

        public T setSequenceNumber(int packetNumber) {
            this.packetNumber = packetNumber;
            return (T) this;
        }

        /**
         * @noinspection UnusedReturnValue
         */
        public T setPacketType(int packetType) {
            this.packetType = packetType;
            return (T) this;
        }

        public abstract XiaomiSppPacketV2 build();
    }

    public static class AckPacket extends XiaomiSppPacketV2 {
        public static class Builder extends XiaomiSppPacketV2.Builder<Builder> {
            public Builder() {
                setPacketType(PACKET_TYPE_ACK);
            }

            @Override
            public XiaomiSppPacketV2 build() {
                return new AckPacket(this);
            }
        }

        protected AckPacket(final Builder builder) {
            super(builder.packetType, builder.packetNumber);
        }

        @Override
        protected byte[] getPacketPayloadBytes(XiaomiAuthService authService) {
            return new byte[0];
        }
    }

    public static class SessionConfigPacket extends XiaomiSppPacketV2 {
        public static final int OPCODE_START_SESSION_REQUEST = 1;
        public static final int OPCODE_START_SESSION_RESPONSE = 2;
        public static final int OPCODE_STOP_SESSION_REQUEST = 3;
        public static final int OPCODE_STOP_SESSION_RESPONSE = 4;

        public static final int KEY_VERSION = 1;
        public static final int KEY_MAX_PACKET_SIZE = 2;
        public static final int KEY_TX_WIN = 3;
        public static final int KEY_SEND_TIMEOUT = 4;

        private static final int VALUE_SIZE_VERSION = 3;
        private static final int VALUE_SIZE_MAX_PACKET_SIZE = 2;
        private static final int VALUE_SIZE_TX_WIN = 2;
        private static final int VALUE_SIZE_SEND_TIMEOUT = 2;

        public static class Builder extends XiaomiSppPacketV2.Builder<Builder> {
            private int opCode = -1;

            public Builder() {
                setPacketType(PACKET_TYPE_SESSION_CONFIG);
            }

            public Builder setOpCode(final int opCode) {
                this.opCode = opCode;
                return this;
            }

            @Override
            public XiaomiSppPacketV2 build() {
                return new SessionConfigPacket(this);
            }
        }

        private final int opCode;

        protected SessionConfigPacket(final Builder builder) {
            super(builder.packetType, builder.packetNumber);
            this.opCode = builder.opCode;
        }

        public int getOpCode() {
            return this.opCode;
        }

        @Override
        protected byte[] getPacketPayloadBytes(XiaomiAuthService authService) {
            // from packet dump of official app
            return new byte[]{
                    // opcode
                    (byte) this.opCode,

                    // VERSION (type 1) = 01.00.00
                    KEY_VERSION,
                    0x03, 0x00,
                    0x01, 0x00, 0x00,

                    // MAX_FRAME_SIZE (type 2) = 0xfc00 -> 64512 bytes
                    KEY_MAX_PACKET_SIZE,
                    0x02, 0x00,
                    0x00, (byte) 0xfc,

                    // TX_WIN (type 3) = 0x0020 -> 32 frames
                    KEY_TX_WIN,
                    0x02, 0x00,
                    0x20, 0x00,

                    // SEND_TIMEOUT (type 4) = 0x2710 -> 10000ms
                    KEY_SEND_TIMEOUT,
                    0x02,
                    0x10, 0x27,
            };
        }

        public static XiaomiSppPacketV2 decodePayloadBytes(final int sequenceNumber, final byte[] payloadBytes) {
            final ByteBuffer buffer = ByteBuffer.wrap(payloadBytes).order(ByteOrder.LITTLE_ENDIAN);

            if (buffer.remaining() < 1) {
                LOG.warn("SessionConfig.decodePayloadBytes(): at least 1 byte required to decode");
                return null;
            }

            final int opCode = buffer.get() & 0xff;

            switch (opCode) {
                case OPCODE_START_SESSION_REQUEST:
                case OPCODE_START_SESSION_RESPONSE: {
                    while (buffer.remaining() >= 3) {
                        final int key = buffer.get() & 0xff;
                        final int valueSize = buffer.getShort() & 0xffff;

                        if (buffer.remaining() < valueSize) {
                            LOG.warn("not enough bytes remaining to extract value");
                            break;
                        }

                        // TODO store and handle values
                        switch (key) {
                            case KEY_VERSION: {
                                if (valueSize != VALUE_SIZE_VERSION) {
                                    LOG.warn("expected {} bytes for version value, got {}", VALUE_SIZE_VERSION, valueSize);
                                    buffer.get(new byte[valueSize]);
                                    break;
                                }

                                final byte[] version = new byte[valueSize];
                                buffer.get(version);
                                LOG.debug("received SPPv2 version: {}", GB.hexdump(version));
                                break;
                            }
                            case KEY_MAX_PACKET_SIZE: {
                                if (valueSize != VALUE_SIZE_MAX_PACKET_SIZE) {
                                    LOG.warn("expected 2 bytes for maximum packet size, got {}", valueSize);
                                    buffer.get(new byte[valueSize]);
                                    break;
                                }

                                LOG.debug("received max packet size: {}", buffer.getShort() & 0xffff);
                                break;
                            }
                            case KEY_TX_WIN: {
                                if (valueSize != VALUE_SIZE_TX_WIN) {
                                    LOG.warn("expected {} bytes for transmission window, got {}", VALUE_SIZE_TX_WIN, valueSize);
                                    buffer.get(new byte[valueSize]);
                                    break;
                                }

                                LOG.debug("received tx win: {}", buffer.getShort() & 0xffff);
                                break;
                            }
                            case KEY_SEND_TIMEOUT: {
                                if (valueSize != VALUE_SIZE_SEND_TIMEOUT) {
                                    LOG.warn("expected {} bytes for send timeout value, got {}", VALUE_SIZE_SEND_TIMEOUT, valueSize);
                                    buffer.get(new byte[valueSize]);
                                    break;
                                }

                                LOG.debug("received send timeout: {}ms", buffer.getShort() & 0xffff);
                                break;
                            }
                            default: {
                                final byte[] value = new byte[valueSize];
                                LOG.debug("received unknown config type {} with byte value {}",
                                        key,
                                        GB.hexdump(value));
                                break;
                            }
                        }
                    }

                    break;
                }
                case OPCODE_STOP_SESSION_REQUEST:
                case OPCODE_STOP_SESSION_RESPONSE: {
                    break;
                }
                default: {
                    LOG.error("SessionConfigPacket#decode(): unknown opcode {}", opCode);
                    break;
                }
            }

            return new Builder()
                    .setSequenceNumber(sequenceNumber)
                    .setOpCode(opCode)
                    .build();
        }
    }

    public static class DataPacket extends XiaomiSppPacketV2 {
        private static final int CHANNEL_UNKNOWN = -1;
        private static final int CHANNEL_PROTOBUF = 1; // encrypted after authentication
        private static final int CHANNEL_DATA = 2; // not encrypted
        private static final int CHANNEL_ACTIVITY = 5; // encrypted

        public static final int OPCODE_UNKNOWN = -1;
        public static final int OPCODE_SEND_PLAINTEXT = 1;
        public static final int OPCODE_SEND_ENCRYPTED = 2;

        public static class Builder extends XiaomiSppPacketV2.Builder<Builder> {
            private Channel channel = Channel.Unknown;
            private int opCode = OPCODE_UNKNOWN;
            private byte[] payload = new byte[0];

            public Builder() {
                setPacketType(PACKET_TYPE_DATA);
            }

            public Builder setOpCode(final int opCode) {
                this.opCode = opCode;
                return this;
            }

            public Builder setChannel(final Channel channel) {
                this.channel = channel;
                return this;
            }

            public Builder setPayload(final byte[] payload) {
                this.payload = payload;
                return this;
            }

            public XiaomiSppPacketV2 build() {
                return new DataPacket(this);
            }
        }

        private final Channel channel;
        private final int opCode;
        private final byte[] payload;

        protected DataPacket(final Builder builder) {
            super(builder.packetType, builder.packetNumber);
            this.channel = builder.channel;
            this.opCode = builder.opCode;
            this.payload = builder.payload;
        }

        private static byte getRawChannel(final Channel channel) {
            switch (channel) {
                case Authentication: // fall through
                case ProtobufCommand:
                    return CHANNEL_PROTOBUF;
                case Data:
                    return CHANNEL_DATA;
                case Activity:
                    return CHANNEL_ACTIVITY;
                default:
                    LOG.warn("getRawChannel(): unable to get raw channel value for channel '{}'", channel);
                    return CHANNEL_UNKNOWN;
            }
        }

        private static Channel getChannelFromRaw(final int rawChannel) {
            switch (rawChannel) {
                case CHANNEL_PROTOBUF:
                    return Channel.ProtobufCommand;
                case CHANNEL_ACTIVITY:
                    return Channel.Activity;
                case CHANNEL_DATA:
                    return Channel.Data;
                default:
                    LOG.warn("getChannelFromRaw(): unknown raw channel {}", rawChannel);
                    return Channel.Unknown;
            }
        }

        public static int getOpCodeForChannel(final Channel channel) {
            switch (channel) {
                case Authentication:
                case Data:
                    return OPCODE_SEND_PLAINTEXT;
                case ProtobufCommand:
                case Activity:
                    return OPCODE_SEND_ENCRYPTED;
                default:
                    LOG.warn("getOpCodeForChannel(): conversion for channel {} unknown", channel);
                    return OPCODE_UNKNOWN;
            }
        }

        public static XiaomiSppPacketV2 decodePacketPayload(int sequenceNumber, byte[] payloadBytes) {
            if (payloadBytes == null || payloadBytes.length < 2) {
                LOG.error("DataPacket.decodePacketPayload(): not enough bytes to decode data packet payload");
                return null;
            }

            final ByteBuffer buffer = ByteBuffer.wrap(payloadBytes).order(ByteOrder.LITTLE_ENDIAN);
            final int rawChannel = buffer.get() & 0xf;
            final int opCode = buffer.get() & 0xff;
            final byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);

            return new Builder()
                    .setSequenceNumber(sequenceNumber)
                    .setChannel(getChannelFromRaw(rawChannel))
                    .setOpCode(opCode)
                    .setPayload(payload)
                    .build();
        }

        @Override
        protected byte[] getPacketPayloadBytes(XiaomiAuthService authService) {
            final ByteBuffer buffer = ByteBuffer.allocate(2 + payload.length);
            buffer.put((byte) (getRawChannel(this.channel) & 0xf));
            buffer.put((byte) (opCode & 0xff));
            buffer.put(opCode == OPCODE_SEND_ENCRYPTED ? authService.encryptV2(payload) : payload);
            return buffer.array();
        }

        public Channel getChannel() {
            return channel;
        }

        public byte[] getPayloadBytes(final XiaomiAuthService authService) {
            if (this.opCode == OPCODE_SEND_ENCRYPTED) {
                return authService.decryptV2(this.payload);
            }

            return this.payload;
        }
    }

    protected XiaomiSppPacketV2(final int packetType, final int sequenceNumber) {
        this.packetType = packetType;
        this.sequenceNumber = sequenceNumber;
    }

    public static SessionConfigPacket.Builder newSessionConfigPacketBuilder() {
        return new SessionConfigPacket.Builder();
    }

    public static DataPacket.Builder newDataPacketBuilder() {
        return new DataPacket.Builder();
    }

    public int getPacketType() {
        return this.packetType;
    }

    public int getSequenceNumber() {
        return this.sequenceNumber;
    }

    private static int calculatePayloadChecksum(final byte[] payload) {
        // consider moving to nodomain.freeyourgadget.gadgetbridge.util.CheckSums
        // configuration: CRC-16/ARC (poly=0x8005, init=0, xorout=0. refin, refout)
        int crc = 0;
        for (final byte b : payload) {
            for (int j = 0; j < 8; j++) {
                crc <<= 1;
                if ((((crc >> 16) & 1) ^ ((b >> j) & 1)) == 1)
                    crc ^= 0x8005;
            }
        }
        return (Integer.reverse(crc) >>> 16);
    }

    public byte[] encode(final XiaomiAuthService authService) {
        final byte[] payloadBytes = getPacketPayloadBytes(authService);
        final ByteBuffer buffer = ByteBuffer.allocate(8 + payloadBytes.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(PACKET_PREAMBLE);
        buffer.put((byte) (packetType & 0xf));
        buffer.put((byte) (sequenceNumber & 0xff));
        buffer.putShort((short) payloadBytes.length);
        buffer.putShort((short) calculatePayloadChecksum(payloadBytes));
        buffer.put(payloadBytes);
        return buffer.array();
    }

    public static XiaomiSppPacketV2 decode(final byte[] packetBytes) {
        if (packetBytes.length < 8) {
            // caller should have checked if a full packet is in the given buffer
            LOG.warn("decode(): at least 8 bytes required, got {}", packetBytes.length);
            return null;
        }

        final ByteBuffer packetBuffer = ByteBuffer.wrap(packetBytes).order(ByteOrder.LITTLE_ENDIAN);

        // verify packet preamble
        {
            final byte[] preamble = new byte[PACKET_PREAMBLE.length];
            packetBuffer.get(preamble);
            if (!Arrays.equals(PACKET_PREAMBLE, preamble)) {
                LOG.error("decode(): packet header mismatch: expected {}, got {}", GB.hexdump(PACKET_PREAMBLE), GB.hexdump(preamble));
                return null;
            }
        }

        final int packetType, sequenceNumber, payloadLength, givenChecksum;
        final byte[] payloadBytes;

        // extract header fields and verify all bytes present
        {
            final byte b = packetBuffer.get(); // flags and packet type
            // TODO process flags
            packetType = b & 0xf;
            sequenceNumber = packetBuffer.get() & 0xff;
            payloadLength = packetBuffer.getShort() & 0xffff;
            givenChecksum = packetBuffer.getShort() & 0xffff;

            if (packetBuffer.remaining() < payloadLength) {
                LOG.error("decode(): expected at least {} bytes in buffer, got {} (missing {} bytes to complete packet)",
                        payloadLength + 8,
                        packetBytes.length,
                        payloadLength - packetBuffer.remaining());
                return null;
            }
        }

        // get payload and verify checksum
        {
            payloadBytes = new byte[payloadLength];
            packetBuffer.get(payloadBytes);
            final int calculatedChecksum = calculatePayloadChecksum(payloadBytes);

            if (calculatedChecksum != givenChecksum) {
                LOG.error("decode(): payload checksum mismatch (given {} != calculated {})",
                        givenChecksum,
                        calculatedChecksum);
                return null;
            }
        }

        final XiaomiSppPacketV2 decodedPacket;

        switch (packetType) {
            case PACKET_TYPE_SESSION_CONFIG:
                decodedPacket = SessionConfigPacket.decodePayloadBytes(sequenceNumber, payloadBytes);
                break;
            case PACKET_TYPE_DATA:
                decodedPacket = DataPacket.decodePacketPayload(sequenceNumber, payloadBytes);
                break;
            case PACKET_TYPE_ACK:
                decodedPacket = new AckPacket.Builder()
                        .setSequenceNumber(sequenceNumber)
                        .build();
                break;
            default:
                LOG.warn("decode(): unhandled packet type {}", packetType);
                decodedPacket = null;
                break;
        }

        return decodedPacket;
    }
}
