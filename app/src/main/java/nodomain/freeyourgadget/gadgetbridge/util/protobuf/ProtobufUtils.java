/*  Copyright (C) 2022-2024 Daniel Dakhno

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
package nodomain.freeyourgadget.gadgetbridge.util.protobuf;

import java.io.ByteArrayOutputStream;

public class ProtobufUtils {
    public static byte[] encode_varint(int value){
        if(value == 0){
            return new byte[]{0x00};
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4);

        while(value > 0){
            byte newByte = (byte)(value & 0b01111111);
            value >>= 7;
            if(value != 0){
                newByte |= 0b10000000;
            }
            bytes.write(newByte);
        }

        return bytes.toByteArray();
    }
}
