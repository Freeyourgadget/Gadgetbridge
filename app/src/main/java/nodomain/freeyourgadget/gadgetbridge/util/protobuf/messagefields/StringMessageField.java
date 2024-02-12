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

import nodomain.freeyourgadget.gadgetbridge.util.protobuf.ProtobufUtils;

public class StringMessageField extends MessageField{
    String fieldValueString;

    public StringMessageField(int fieldNumber, String value) {
        super(fieldNumber, FieldType.LENGTH_DELIMITED);
        fieldValueString = value;
    }

    @Override
    public void encode(ByteArrayOutputStream os) throws IOException {
        os.write(getStartBytes());

        os.write(ProtobufUtils.encode_varint(fieldValueString.length()));

        os.write(fieldValueString.getBytes());
    }
}
