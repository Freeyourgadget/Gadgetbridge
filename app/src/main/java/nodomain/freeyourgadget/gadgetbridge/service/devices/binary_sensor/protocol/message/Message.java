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
package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;

public class Message {
    MessageId messageId;
    Parameter[] parameters;

    public Message(MessageId messageId, Parameter[] parameters) {
        this.messageId = messageId;
        this.parameters = parameters;
    }

    public byte[] encode(){
        int dataLength = 4;
        for(Parameter parameter : parameters){
            dataLength += parameter.getPayloadLength() + 4;
        }
        ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        buffer
                .put((byte) 0x00) // RFU
                .put(messageId.getMessageIdByte()) // RFU
                .put((byte) 0x00) // RFU
                .put((byte) parameters.length);
        for(Parameter parameter : parameters){
            buffer.put(parameter.encode());
        }
        return buffer.array();
    }
}
