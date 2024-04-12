package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.HashMap;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationControlStatusMessage;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.GET_NOTIFICATION_ATTRIBUTES;

public class NotificationControlMessage extends GFDIMessage {

    private final NotificationsHandler.NotificationCommand command;
    private final int notificationId;
    private final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap;

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.notificationAttributesMap = notificationAttributesMap;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);
    }

    public static NotificationControlMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {


        final NotificationsHandler.NotificationCommand command = NotificationsHandler.NotificationCommand.fromCode(reader.readByte());
        if (command != GET_NOTIFICATION_ATTRIBUTES) {
            LOG.error("NOT SUPPORTED");

        }
        LOG.info("COMMAND: {}", command.ordinal());
        final int notificationId = reader.readInt();
        final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap = createGetNotificationAttributesCommand(reader);

        return new NotificationControlMessage(garminMessage, command, notificationId, notificationAttributesMap);
    }

    private static Map<NotificationsHandler.NotificationAttribute, Integer> createGetNotificationAttributesCommand(MessageReader reader) {
        final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap = new HashMap<>();
        while (reader.remaining() > 0) {
            final int attributeID = reader.readByte();

            final NotificationsHandler.NotificationAttribute attribute = NotificationsHandler.NotificationAttribute.getByCode(attributeID);
            LOG.info("Requested attribute: {}", attribute);
            if (attribute == null) {
                LOG.error("Unknown notification attribute {}", attributeID);
                return null;
            }
            final int maxLength;
            if (attribute.hasLengthParam) {
                maxLength = reader.readShort();

            } else if (attribute.hasAdditionalParams) {
                maxLength = reader.readShort();
                // TODO: What is this??
                reader.readByte();

            } else {
                maxLength = 0;
            }
            notificationAttributesMap.put(attribute, maxLength);
        }
        return notificationAttributesMap;
    }

    public NotificationsHandler.NotificationCommand getCommand() {
        return command;
    }

    public int getNotificationId() {
        return notificationId;
    }

    public Map<NotificationsHandler.NotificationAttribute, Integer> getNotificationAttributesMap() {
        return notificationAttributesMap;
    }

    @Override
    protected boolean generateOutgoing() {
        return false;
    }

}
