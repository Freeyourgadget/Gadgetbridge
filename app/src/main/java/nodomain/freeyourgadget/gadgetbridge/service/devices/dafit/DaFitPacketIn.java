/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.dafit;

import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.Logging;

/**
 * A class for handling fragmentation of incoming packets<br>
 * <br>
 * Usage:
 * <pre>
 * {@code
 * if(packetIn.putFragment(fragment)) {
 *     Pair<Byte, byte[]> packet = DaFitPacketIn.parsePacket(packetIn.getPacket());
 *     packetIn = new DaFitPacketIn();
 *     if (packet != null) {
 *         byte packetType = packet.first;
 *         byte[] payload = packet.second;
 *         // ...
 *     }
 * }
 * </pre>
 */
public class DaFitPacketIn extends DaFitPacket {
    private static final Logger LOG = LoggerFactory.getLogger(DaFitPacketIn.class);

    public DaFitPacketIn()
    {

    }

    /**
     * Store the incoming fragment and try to reconstruct packet
     *
     * @param fragment The incoming fragment
     * @return true if the packet is complete
     */
    public boolean putFragment(byte[] fragment)
    {
        if (packet == null)
        {
            int len = parsePacketLength(fragment);
            if (len < 0)
                return false; // corrupted packet
            packet = new byte[len];
        }

        int toCopy = Math.min(fragment.length, packet.length - position);
        if (fragment.length > toCopy)
        {
            LOG.warn("Got fragment with more data than expected!");
        }

        System.arraycopy(fragment, 0, packet, position, toCopy);
        position += fragment.length;
        return position >= packet.length;
    }

    public byte[] getPacket()
    {
        if (packet == null || position < packet.length)
            throw new IllegalStateException("Packet is not complete yet");
        return packet;
    }

    /**
     * Parse the packet header and return the length
     * @param packetOrFragment The entire packet or it's first fragment
     * @return The packet length, or -1 if packet is corrupted
     */
    private static int parsePacketLength(@NonNull byte[] packetOrFragment)
    {
        if (packetOrFragment[0] != (byte)0xFE || packetOrFragment[1] != (byte)0xEA)
        {
            LOG.warn("Invalid packet header, ignoring! Fragment: " + Logging.formatBytes(packetOrFragment));
            return -1;
        }

        int len_h = 0;
        if (packetOrFragment[2] != 16)
        {
            if ((packetOrFragment[2] & 0xFF) < 32)
            {
                LOG.warn("Corrupted packet, unable to parse length");
                return -1;
            }
            len_h = (packetOrFragment[2] & 0xFF) - 32;
        }
        int len_l = (packetOrFragment[3] & 0xFF);

        return (len_h << 8) | len_l;
    }

    /**
     * Parse the packet
     * @param packet The complete packet
     * @return A pair containing the packet type and payload
     */
    public static Pair<Byte, byte[]> parsePacket(@NonNull byte[] packet)
    {
        int len = parsePacketLength(packet);
        if (len < 0)
            return null;
        if (len != packet.length)
        {
            LOG.warn("Invalid packet length!");
            return null;
        }
        byte packetType = packet[4];
        byte[] payload = new byte[packet.length - 5];
        System.arraycopy(packet, 5, payload, 0, payload.length);
        return Pair.create(packetType, payload);
    }
}
