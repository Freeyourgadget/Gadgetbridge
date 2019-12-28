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

import androidx.annotation.NonNull;

/**
 * A class for handling fragmentation of outgoing packets<br>
 * <br>
 * Usage:
 * <pre>
 * {@code
 * DaFitPacketOut packetOut = new DaFitPacketOut(DaFitPacketOut.buildPacket(type, payload));
 * byte[] fragment = new byte[MTU];
 * while(packetOut.getFragment(fragment))
 *     send(fragment);
 * }
 * </pre>
 */
public class DaFitPacketOut extends DaFitPacket {
    public DaFitPacketOut(byte[] packet)
    {
        this.packet = packet;
    }

    /**
     * Get the next fragment of this packet to be sent
     *
     * @param fragmentBuffer The buffer to store the output in, of desired size (i.e. == MTU)
     * @return true if there is more data to be sent, false otherwise
     */
    public boolean getFragment(byte[] fragmentBuffer)
    {
        if (position >= packet.length)
            return false;
        int remainingToTransfer = Math.min(fragmentBuffer.length, packet.length - position);
        System.arraycopy(packet, position, fragmentBuffer, 0, remainingToTransfer);
        position += remainingToTransfer;
        return true;
    }

    /**
     * Encode the packet
     * @param packetType The packet type
     * @param payload The packet payload
     * @return The encoded packet
     */
    public static byte[] buildPacket(byte packetType, @NonNull byte[] payload)
    {
        byte[] packet = new byte[payload.length + 5];
        packet[0] = (byte)0xFE;
        packet[1] = (byte)0xEA;
        if (DaFitDeviceSupport.MTU == 20)
        {
            packet[2] = 16;
            packet[3] = (byte)(packet.length & 0xFF);
        }
        else
        {
            packet[2] = (byte)(32 + (packet.length >> 8) & 0xFF);
            packet[3] = (byte)(packet.length & 0xFF);
        }
        packet[4] = packetType;
        System.arraycopy(payload, 0, packet, 5, payload.length);
        return packet;
    }
}
