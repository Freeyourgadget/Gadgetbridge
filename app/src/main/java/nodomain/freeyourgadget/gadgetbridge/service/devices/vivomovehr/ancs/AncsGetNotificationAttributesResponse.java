package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AncsGetNotificationAttributesResponse {
    public final byte[] packet;

    public AncsGetNotificationAttributesResponse(int notificationUID, Map<AncsAttribute, String> attributes) {
        final MessageWriter messageWriter = new MessageWriter();
        messageWriter.writeByte(AncsCommand.GET_NOTIFICATION_ATTRIBUTES.code);
        messageWriter.writeInt(notificationUID);
        for(Map.Entry<AncsAttribute, String> attribute : attributes.entrySet()) {
            messageWriter.writeByte(attribute.getKey().code);
            final byte[] bytes = attribute.getValue().getBytes(StandardCharsets.UTF_8);
            messageWriter.writeShort(bytes.length);
            messageWriter.writeBytes(bytes);
        }
        this.packet = messageWriter.getBytes();
    }
}
