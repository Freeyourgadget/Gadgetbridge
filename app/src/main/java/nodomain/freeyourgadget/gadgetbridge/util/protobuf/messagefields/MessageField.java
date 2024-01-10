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
package nodomain.freeyourgadget.gadgetbridge.util.protobuf.messagefields;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public abstract class MessageField {
    public static enum FieldType{
        VARINT,
        INT_64_BIT,
        LENGTH_DELIMITED,
        START_GROUP,
        END_GROUP,
        INT_32_BIT
    }

    int fieldNumber;
    FieldType fieldType;

    protected MessageField(int fieldNumber, FieldType fieldType){
        this.fieldNumber = fieldNumber;
        this.fieldType = fieldType;
    }

    protected byte[] getStartBytes(){
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(4);

        byte result = (byte) (fieldType.ordinal() & 0b111);

        int fieldNumber = this.fieldNumber;

        result |= (fieldNumber & 0b1111) << 3;
        fieldNumber >>= 4;
        if(fieldNumber > 0){
            result |= 0b10000000;
        }
        bytes.write(result);

        while(fieldNumber > 0){
            byte newByte = (byte)(fieldNumber & 0b01111111);
            fieldNumber >>= 7;
            if(fieldNumber != 0){
                newByte |= 0b10000000;
            }
            bytes.write(newByte);
        }

        return bytes.toByteArray();
    }

    abstract public void encode(ByteArrayOutputStream os) throws IOException;

    public byte[] encodeToBytes() throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encode(os);
        return os.toByteArray();
    }
}
