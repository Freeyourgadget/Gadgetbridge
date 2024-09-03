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
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.OPCODE_SEND;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.PACKET_PREAMBLE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV1.getDataTypeForChannel;

public class XiaomiSppProtocolV1 extends AbstractXiaomiSppProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSppProtocolV1.class);

    private final XiaomiSppSupport support;
    private final AtomicInteger frameCounter = new AtomicInteger(0);
    private final AtomicInteger encryptionCounter = new AtomicInteger(0);

    public XiaomiSppProtocolV1(XiaomiSppSupport support) {
        this.support = support;
    }

    @Override
    public int findNextPacketOffset(byte[] buffer) {
        for (int i = 1; i < buffer.length; i++) {
            // just check for the first byte, the processPacket method checks the full magic
            if (buffer[i] == PACKET_PREAMBLE[0]) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public ParseResult processPacket(byte[] buffer) {
        if (buffer.length < 11) {
            LOG.debug("processPacket(): not enough bytes in rx buffer to decode packet header");
            return new ParseResult(ParseResult.Status.Incomplete);
        }

        final ByteBuffer headerBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN);
        final int packetSize;

        // verify preamble
        {
            byte[] preamble = new byte[PACKET_PREAMBLE.length];
            headerBuffer.get(preamble);

            if (!Arrays.equals(PACKET_PREAMBLE, preamble)) {
                LOG.debug("processPacket(): header mismatch, expected {}, got {}",
                        GB.hexdump(PACKET_PREAMBLE),
                        GB.hexdump(preamble));
                return new ParseResult(ParseResult.Status.Invalid);
            }
        }

        // verify packet size
        {
            headerBuffer.getShort(); // skip flags and channel ID
            int payloadSize = headerBuffer.getShort() & 0xffff;
            packetSize = payloadSize + 8; // payload size includes payload header

            if (buffer.length < packetSize) {
                LOG.debug("processPacket(): received {}, missing {}/{} packet bytes",
                        buffer.length,
                        packetSize - buffer.length,
                        packetSize);
                return new ParseResult(ParseResult.Status.Incomplete);
            }

            LOG.debug("processPacket(): all bytes for packet of {} bytes in buffer", packetSize);
        }

        XiaomiSppPacketV1 receivedPacket = XiaomiSppPacketV1.decode(buffer);

        if (receivedPacket == null) {
            LOG.debug("processPacket(): decoded packet is null");
            return new ParseResult(ParseResult.Status.Invalid);
        }

        LOG.debug("processPacket(): Packet received: {}", receivedPacket);
        support.onPacketReceived(receivedPacket.getChannel(), receivedPacket.getDecryptedPayload(support.getAuthService()));
        // TODO send response if requested by device
        return new ParseResult(ParseResult.Status.Complete, packetSize);
    }

    @Override
    public byte[] encodePacket(XiaomiChannelHandler.Channel channel, byte[] data) {
        return XiaomiSppPacketV1.newBuilder()
                .channel(channel)
                .opCode(OPCODE_SEND)
                .frameSerial(frameCounter.getAndIncrement())
                .dataType(getDataTypeForChannel(channel))
                .payload(data)
                .build()
                .encode(support.getAuthService(), encryptionCounter);
    }
}
