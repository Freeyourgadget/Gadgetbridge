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
