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
package nodomain.freeyourgadget.gadgetbridge.service.devices.moondrop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static nodomain.freeyourgadget.gadgetbridge.util.GB.hexdump;

public class GaiaPacket {
    private static final Logger LOG = LoggerFactory.getLogger(GaiaPacket.class);

    public static final byte PDU_COMMAND = (byte)0x00;
    public static final byte PDU_NOTIFICATION = (byte)0x01;
    public static final byte PDU_RESPONSE = (byte)0x02;
    public static final byte PDU_ERROR = (byte)0x03;

    private static final int HEADER_LENGTH = 8;
    private static final byte START_OF_PACKET = (byte)0xff;
    private static final byte VERSION = (byte)0x04;
    private static final byte FLAGS_NO_CHECK = (byte)0x00;

    // Qualcomm's vendor ID
    private static final short VENDOR_ID = 0x001d;

    private byte featureId;
    private byte pduType;
    private byte pduId;

    private byte[] payload;

    public GaiaPacket(byte featureId, byte pduId) {
        this(featureId, pduId, new byte[] {});
    }

    public GaiaPacket(byte featureId, byte pduId, byte[] payload) {
        this(featureId, PDU_COMMAND, pduId, payload);
    }

    private GaiaPacket(byte featureId, byte pduType, byte pduId, byte[] payload) {
        LOG.debug(
                "Packet: featureId={}, pduType={}, pduId={}, length={}",
                String.format("0x%02x", featureId),
                String.format("0x%02x", pduType),
                String.format("0x%02x", pduId),
                payload.length);

        this.featureId = featureId;
        this.pduType = pduType;
        this.pduId = pduId;
        this.payload = payload;
    }

    public byte getFeatureId() {
        return featureId;
    }

    public byte getPduType() {
        return pduType;
    }

    public byte getPduId() {
        return pduId;
    }

    public byte[] getPayload() {
        return payload;
    }

    public static GaiaPacket decode(ByteBuffer buf) {
        if (buf.remaining() < HEADER_LENGTH)
            return null;

        if (buf.get() != START_OF_PACKET) {
            LOG.error("Invalid GAIA start of packet: {}", hexdump(buf.array()));
            return null;
        }

        if (buf.get() != VERSION) {
            LOG.error("Invalid GAIA version: {}", hexdump(buf.array()));
            return null;
        }

        if (buf.get() != FLAGS_NO_CHECK) {
            LOG.error("Invalid GAIA flags: {}", hexdump(buf.array()));
            return null;
        }

        byte length = buf.get();

        if (buf.getShort() != VENDOR_ID) {
            LOG.error("Invalid GAIA vendor ID: {}", hexdump(buf.array()));
            return null;
        }

        short commandId = buf.getShort();
        byte featureId = (byte)((commandId & 0xfe00) >> 9);
        byte pduType = (byte)((commandId & 0x0180) >> 7);
        byte pduId = (byte)(commandId & 0x7f);

        byte[] payload = new byte[length];
        buf.get(payload);

        return new GaiaPacket(featureId, pduType, pduId, payload);
    }

    public byte[] encode() {
        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH + payload.length);

        buf.put(START_OF_PACKET);
        buf.put(VERSION);
        buf.put(FLAGS_NO_CHECK);
        buf.put((byte) payload.length);
        buf.putShort(VENDOR_ID);
        buf.putShort((short)((featureId << 9) | (pduType << 7) | pduId));
        buf.put(payload);

        return buf.array();
    }
}
