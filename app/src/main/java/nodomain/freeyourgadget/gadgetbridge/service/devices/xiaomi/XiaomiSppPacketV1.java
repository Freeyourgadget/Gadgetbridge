/*  Copyright (C) 2023-2024 Yoran Vulker

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

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.proto.xiaomi.XiaomiProto;
import nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiChannelHandler.Channel;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class XiaomiSppPacketV1 {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSppPacketV1.class);

    public static final byte[] PACKET_PREAMBLE = new byte[]{(byte) 0xba, (byte) 0xdc, (byte) 0xfe};
    public static final byte[] PACKET_EPILOGUE = new byte[]{(byte) 0xef};

    public static final int CHANNEL_VERSION = 0;
    /**
     * Channel ID for PROTO messages received from device
     */
    public static final int CHANNEL_PROTO_RX = 1;

    /**
     * Channel ID for PROTO messages sent to device
     */
    public static final int CHANNEL_PROTO_TX = 2;
    public static final int CHANNEL_FITNESS = 3;
    public static final int CHANNEL_VOICE = 4;
    public static final int CHANNEL_MASS = 5;
    public static final int CHANNEL_OTA = 7;

    public static final int DATA_TYPE_PLAIN = 0;
    public static final int DATA_TYPE_ENCRYPTED = 1;
    public static final int DATA_TYPE_AUTH = 2;

    public static final int OPCODE_READ = 0;
    public static final int OPCODE_SEND = 2;

    private byte[] payload;
    private boolean flag, needsResponse;
    private Channel channel;
    private int rawChannel, opCode, frameSerial, dataType;

    public static int getDataTypeForChannel(final Channel channel) {
        switch (channel) {
            case Authentication:
                return DATA_TYPE_AUTH;
            case ProtobufCommand:
            case Version:
            case Data:
                return DATA_TYPE_ENCRYPTED;
            default:
                LOG.warn("getDataTypeForChannel(): cannot determine data type for channel {}", channel);
                // fall through
            case Activity: // and voice
                return DATA_TYPE_PLAIN;
        }
    }

    public static class Builder {
        private byte[] payload = new byte[0];
        private boolean flag = true, needsResponse = false;
        private Channel channel = Channel.Unknown;
        private int opCode = -1, frameSerial = -1, dataType = -1;

        public XiaomiSppPacketV1 build() {
            XiaomiSppPacketV1 result = new XiaomiSppPacketV1();

            result.channel = channel;
            result.flag = flag;
            result.needsResponse = needsResponse;
            result.opCode = opCode;
            result.frameSerial = frameSerial;
            result.dataType = dataType;
            result.payload = payload;
            result.rawChannel = getRawChannel(channel, true);

            return result;
        }

        public Builder channel(final Channel channel) {
            this.channel = channel;
            return this;
        }

        public Builder flag(final boolean flag) {
            this.flag = flag;
            return this;
        }

        public Builder needsResponse(final boolean needsResponse) {
            this.needsResponse = needsResponse;
            return this;
        }

        public Builder opCode(final int opCode) {
            this.opCode = opCode;
            return this;
        }

        public Builder frameSerial(final int frameSerial) {
            this.frameSerial = frameSerial;
            return this;
        }

        public Builder dataType(final int dataType) {
            this.dataType = dataType;
            return this;
        }

        public Builder payload(final byte[] payload) {
            this.payload = payload;
            return this;
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public int getDataType() {
        return dataType;
    }

    public byte[] getPayload() {
        return payload;
    }

    public byte[] getDecryptedPayload(final XiaomiAuthService authService) {
        if (payload == null) {
            LOG.warn("getDecryptedPayload(): payload is null");
            return null;
        }

        if (authService == null) {
            LOG.warn("getDecryptedPayload(): authService is null");
            return payload;
        }

        if (!authService.isEncryptionInitialized() && dataType == DATA_TYPE_ENCRYPTED) {
            LOG.warn("getDecryptedPayload(): authService is not ready to decrypt");
            return payload;
        }

        if (dataType == DATA_TYPE_ENCRYPTED) {
            return authService.decrypt(payload);
        }

        return payload;
    }

    public boolean needsResponse() {
        return needsResponse;
    }

    public boolean hasFlag() {
        return this.flag;
    }

    public static XiaomiSppPacketV1 fromXiaomiCommand(final XiaomiProto.Command command, int frameCounter, boolean needsResponse) {
        return newBuilder().channel(Channel.ProtobufCommand).needsResponse(needsResponse).dataType(
                command.getType() == XiaomiAuthService.COMMAND_TYPE && command.getSubtype() >= 17 ? DATA_TYPE_AUTH : DATA_TYPE_ENCRYPTED
        ).frameSerial(frameCounter).opCode(OPCODE_SEND).payload(command.toByteArray()).build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ROOT,
                "SppPacket{ channel=%s, rawChannel=%d, flag=%b, needsResponse=%b, opCode=0x%x, frameSerial=0x%x, dataType=0x%x, payloadSize=%d }",
                channel, rawChannel, flag, needsResponse, opCode, frameSerial, dataType, payload.length);
    }

    public static int getRawChannel(final Channel channel, final boolean tx) {
        switch (channel) {
            case Version:
                return CHANNEL_VERSION;
            case Authentication:
            case ProtobufCommand:
                return tx ? CHANNEL_PROTO_TX : CHANNEL_PROTO_RX;
            case Activity:
                return CHANNEL_FITNESS;
            case Data:
                return CHANNEL_MASS;
            default:
                LOG.warn("Raw channel for {} unknown", channel);
                return -1;
        }
    }

    public static Channel getChannel(final byte rawChannel) {
        switch (rawChannel & 0xff) {
            case CHANNEL_PROTO_RX:
            case CHANNEL_PROTO_TX:
                return Channel.ProtobufCommand;
            case CHANNEL_FITNESS:
                return Channel.Activity;
            case CHANNEL_MASS:
                return Channel.Data;
            case CHANNEL_VERSION:
                return Channel.Version;
            default:
                LOG.warn("Cannot convert raw channel {} to known channel", rawChannel & 0xff);
                return Channel.Unknown;
        }
    }

    public static XiaomiSppPacketV1 decode(final byte[] packet) {
        if (packet.length < 11) {
            LOG.error("Cannot decode incomplete packet");
            return null;
        }

        ByteBuffer buffer = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        byte[] preamble = new byte[PACKET_PREAMBLE.length];
        buffer.get(preamble);

        if (!Arrays.equals(PACKET_PREAMBLE, preamble)) {
            LOG.error("Expected preamble (0x{}) does not match found preamble (0x{})",
                    GB.hexdump(PACKET_PREAMBLE),
                    GB.hexdump(preamble));
            return null;
        }

        byte channel = buffer.get();

        if ((channel & 0xf0) != 0) {
            LOG.warn("Reserved bits in channel byte are non-zero: 0b{}", Integer.toBinaryString((channel & 0xf0) >> 4));
            channel = 0x0f;
        }

        byte flags = buffer.get();
        boolean flag = (flags & 0x80) != 0;
        boolean needsResponse = (flags & 0x40) != 0;

        if ((flags & 0x0f) != 0) {
            LOG.warn("Reserved bits in flags byte are non-zero: 0b{}", Integer.toBinaryString(flags & 0x0f));
        }

        // payload header is included in size
        int payloadLength = (buffer.getShort() & 0xffff) - 3;

        if (payloadLength + 11 > packet.length) {
            LOG.error("Packet incomplete (expected length: {}, actual length: {})", payloadLength + 11, packet.length);
            return null;
        }

        int opCode = buffer.get() & 0xff;
        int frameSerial = buffer.get() & 0xff;
        int dataType = buffer.get() & 0xff;
        byte[] payload = new byte[payloadLength];
        buffer.get(payload);

        byte[] epilogue = new byte[PACKET_EPILOGUE.length];
        buffer.get(epilogue);

        if (!Arrays.equals(PACKET_EPILOGUE, epilogue)) {
            LOG.error("Expected epilogue (0x{}) does not match actual epilogue (0x{})",
                    GB.hexdump(PACKET_EPILOGUE),
                    GB.hexdump(epilogue));
            return null;
        }

        XiaomiSppPacketV1 result = new XiaomiSppPacketV1();
        result.rawChannel = channel;
        result.channel = getChannel(channel);
        result.flag = flag;
        result.needsResponse = needsResponse;
        result.opCode = opCode;
        result.frameSerial = frameSerial;
        result.dataType = dataType;
        result.payload = payload;

        return result;
    }

    public byte[] encode(final XiaomiAuthService authService, final AtomicInteger encryptionCounter) {
        byte[] payload = this.payload;

        if (dataType == DATA_TYPE_ENCRYPTED && channel == Channel.ProtobufCommand) {
            int packetCounter = encryptionCounter.incrementAndGet();
            payload = authService.encrypt(payload, packetCounter);
            payload = ByteBuffer.allocate(payload.length + 2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) packetCounter).put(payload).array();
        } else if (dataType == DATA_TYPE_ENCRYPTED) {
            payload = authService.encrypt(payload, (short) 0);
        }

        ByteBuffer buffer = ByteBuffer.allocate(11 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(PACKET_PREAMBLE);

        buffer.put((byte) (getRawChannel(channel, true) & 0xf));
        buffer.put((byte) ((flag ? 0x80 : 0) | (needsResponse ? 0x40 : 0)));
        buffer.putShort((short) (payload.length + 3));

        buffer.put((byte) (opCode & 0xff));
        buffer.put((byte) (frameSerial & 0xff));
        buffer.put((byte) (dataType & 0xff));

        buffer.put(payload);

        buffer.put(PACKET_EPILOGUE);
        return buffer.array();
    }
}
