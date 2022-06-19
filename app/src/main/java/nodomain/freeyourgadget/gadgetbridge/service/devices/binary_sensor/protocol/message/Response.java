package nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.message;

import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.constants.MessageId;
import nodomain.freeyourgadget.gadgetbridge.service.devices.binary_sensor.protocol.parameter.Parameter;

public class Response {
    MessageId messageId;
    Parameter[] parameters;

    public MessageId getMessageId() {
        return messageId;
    }

    public Parameter[] getParameters() {
        return parameters;
    }

    public Response(MessageId messageId, Parameter[] parameters) {
        this.messageId = messageId;
        this.parameters = parameters;
    }
}
