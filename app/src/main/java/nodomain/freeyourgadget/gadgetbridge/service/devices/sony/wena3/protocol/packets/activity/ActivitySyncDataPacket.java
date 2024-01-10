/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.packets.activity;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nodomain.freeyourgadget.gadgetbridge.service.devices.sony.wena3.protocol.logic.ActivityPacketCrc;

public class ActivitySyncDataPacket {
    public enum PacketType {
        HEADER,
        DATA,
        FINISH;

        static final PacketType[] LUT = new PacketType[] { HEADER, DATA, FINISH };
    }

    public final int sequenceNo;
    public final PacketType type;
    public int crc;
    public int expectedCrc;
    public boolean isCrcValid;
    public final byte[] data;

    public ActivitySyncDataPacket(byte[] packet) {
        /*
        1byte [seqno OR FF=RESET]
        2byte [crc16]
        1byte [mark: 00=head, 01=data, 02=end]
         IF HEAD
           1byte [type: 00=Steps 01=Heart 02=Behavior 03=Vo2 04=Stress 05=BodyEnergy 06=Calories 07=???]
           (usually) 4byte [WenaDate start ts]
         IF DATA
           (samples of appropriate [type]?)
         IF END
           (reception completed!)
         */
        ByteBuffer buf = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN);
        sequenceNo = Integer.valueOf(buf.get() & 0xFF);
        crc = buf.getShort();
        crc &= 0xFFFF;
        type = PacketType.LUT[buf.get()];
        data = new byte[buf.remaining()];
        buf.get(data);
        checkCrc();
    }

    private void checkCrc() {
        ActivityPacketCrc calc = new ActivityPacketCrc();
        ByteBuffer tmp = ByteBuffer.allocate(data.length + 1).put((byte)type.ordinal()).put(data);
        calc.next(tmp.array());
        expectedCrc = calc.getResult();
        isCrcValid = (expectedCrc == crc);
    }


    @NonNull
    public String toString() {
        return String.format("<ASDP type %d, Seq.No: %d, CRC: %x, Valid: %s, Data: %s>", type.ordinal(), sequenceNo, crc, isCrcValid ? "yes" : String.format("NO [expected %x]", expectedCrc), toHexString(data));
    }

    private static String toHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        if (bytes != null)
            for (byte b:bytes) {
                final String hexString = Integer.toHexString(b & 0xff);
                if(hexString.length()==1)
                    sb.append('0');
                sb.append(hexString).append(' ');
            }
        return sb.toString().toUpperCase();
    }

    public ByteBuffer dataBuffer() {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }
}
