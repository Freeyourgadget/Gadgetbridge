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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import nodomain.freeyourgadget.gadgetbridge.service.btbr.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_PREAMBLE;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_TYPE_ACK;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_TYPE_DATA;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.xiaomi.XiaomiSppPacketV2.PACKET_TYPE_SESSION_CONFIG;

public class XiaomiSppProtocolV2 extends AbstractXiaomiSppProtocol {
    private static final Logger LOG = LoggerFactory.getLogger(XiaomiSppProtocolV2.class);

    private final AtomicInteger packetSequenceCounter = new AtomicInteger(0);
    private final XiaomiSppSupport support;

    public XiaomiSppProtocolV2(final XiaomiSppSupport support) {
        this.support = support;
    }

    private void sendAck(final int sequenceNumber) {
        final TransactionBuilder b = support.commsSupport.createTransactionBuilder(String.format(Locale.ROOT, "send ack for %d", sequenceNumber));
        b.write(new XiaomiSppPacketV2.AckPacket.Builder()
                .setSequenceNumber(sequenceNumber)
                .build()
                .encode(null));
        b.queue(support.commsSupport.getQueue());
    }

    @Override
    public int findNextPacketOffset(byte[] buffer) {
        for (int i = 1; i < buffer.length; i++) {
            if (buffer[i] == PACKET_PREAMBLE[0])
                return i;
        }

        return -1;
    }

    @Override
    public ParseResult processPacket(byte[] rxBuf) {
        if (rxBuf.length < 8) {
            LOG.debug("processPacket(): not enough bytes in buffer to process packet (got {} of required {} bytes)",
                    rxBuf.length,
                    8);
            return new ParseResult(ParseResult.Status.Incomplete);
        }

        final ByteBuffer buffer = ByteBuffer.wrap(rxBuf).order(ByteOrder.LITTLE_ENDIAN);
        final byte[] headerMagic = new byte[PACKET_PREAMBLE.length];
        buffer.get(headerMagic);

        if (!Arrays.equals(PACKET_PREAMBLE, headerMagic)) {
            LOG.warn("processPacket(): invalid header magic (expected {}, got {})",
                    GB.hexdump(PACKET_PREAMBLE),
                    GB.hexdump(headerMagic));
            return new ParseResult(ParseResult.Status.Invalid);
        }

        buffer.get(); // flags and packet type
        buffer.get(); // packet sequence number
        final int packetSize = 8 + (buffer.getShort() & 0xffff);
        buffer.getShort(); // checksum

        if (rxBuf.length < packetSize) {
            LOG.debug("processPacket(): missing {} bytes (got {}/{} bytes)",
                    packetSize - rxBuf.length,
                    rxBuf.length,
                    packetSize);
            return new ParseResult(ParseResult.Status.Incomplete);
        }

        final XiaomiSppPacketV2 decodedPacket = XiaomiSppPacketV2.decode(rxBuf);
        if (decodedPacket != null) {
            switch (decodedPacket.getPacketType()) {
                case PACKET_TYPE_SESSION_CONFIG:
                    // TODO handle device's session config
                    LOG.info("Received session config, opcode={}", ((XiaomiSppPacketV2.SessionConfigPacket)decodedPacket).getOpCode());
                    support.getAuthService().startEncryptedHandshake();
                    break;
                case PACKET_TYPE_DATA:
                    XiaomiSppPacketV2.DataPacket dataPacket = (XiaomiSppPacketV2.DataPacket) decodedPacket;
                    try {
                        support.onPacketReceived(dataPacket.getChannel(), dataPacket.getPayloadBytes(support.getAuthService()));
                    } catch (final Exception ex) {
                        LOG.error("Exception while handling received packet", ex);
                    }
                    // TODO: only directly ack protobuf packets, bulk ack others
                    sendAck(decodedPacket.getSequenceNumber());
                    break;
                case PACKET_TYPE_ACK:
                    LOG.debug("receive ack for packet {}", decodedPacket.getSequenceNumber());
                    break;
                default:
                    LOG.warn("Unhandled packet with type {} (decoded type {})", decodedPacket.getPacketType(), decodedPacket.getClass().getSimpleName());
                    break;
            }
        }

        return new ParseResult(ParseResult.Status.Complete, packetSize);
    }

    @Override
    public boolean initializeSession() {
        final TransactionBuilder builder = support.commsSupport.createTransactionBuilder("send session config");
        builder.write(XiaomiSppPacketV2.newSessionConfigPacketBuilder()
                .setOpCode(XiaomiSppPacketV2.SessionConfigPacket.OPCODE_START_SESSION_REQUEST)
                .setSequenceNumber(0)
                .build()
                .encode(null));
        builder.queue(support.commsSupport.getQueue());
        return false;
    }

    @Override
    public byte[] encodePacket(XiaomiChannelHandler.Channel channel, byte[] payloadBytes) {
        return XiaomiSppPacketV2.newDataPacketBuilder()
                .setChannel(channel)
                .setSequenceNumber(packetSequenceCounter.getAndIncrement())
                .setOpCode(XiaomiSppPacketV2.DataPacket.getOpCodeForChannel(channel))
                .setPayload(payloadBytes)
                .build()
                .encode(support.getAuthService());
    }
}
