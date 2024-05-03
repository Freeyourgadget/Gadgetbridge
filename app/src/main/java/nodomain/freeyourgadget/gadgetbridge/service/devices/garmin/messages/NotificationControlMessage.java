package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEvent;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.NotificationControlStatusMessage;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.GET_NOTIFICATION_ATTRIBUTES;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.PERFORM_LEGACY_NOTIFICATION_ACTION;
import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.NotificationsHandler.NotificationCommand.PERFORM_NOTIFICATION_ACTION;

public class NotificationControlMessage extends GFDIMessage {

    private final NotificationsHandler.NotificationCommand command;
    private final int notificationId;
    private Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap;
    private NotificationsHandler.LegacyNotificationAction legacyNotificationAction;
    private NotificationsHandler.NotificationAction notificationAction;
    private String actionString;
    private List<GBDeviceEvent> gbDeviceEventList;

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, NotificationsHandler.NotificationAction notificationAction, String actionString) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.notificationAction = notificationAction;
        this.actionString = actionString;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);

    }

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, NotificationsHandler.LegacyNotificationAction legacyNotificationAction) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.legacyNotificationAction = legacyNotificationAction;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);
    }

    public NotificationControlMessage(GarminMessage garminMessage, NotificationsHandler.NotificationCommand command, int notificationId, Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap) {
        this.garminMessage = garminMessage;
        this.command = command;
        this.notificationId = notificationId;
        this.notificationAttributesMap = notificationAttributesMap;

        this.statusMessage = new NotificationControlStatusMessage(garminMessage, GFDIMessage.Status.ACK, NotificationControlStatusMessage.NotificationChunkStatus.OK, NotificationControlStatusMessage.NotificationStatusCode.NO_ERROR);
    }

    //TODO: the fact that we return three versions of this object is really ugly
    public static NotificationControlMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {

        final NotificationsHandler.NotificationCommand command = NotificationsHandler.NotificationCommand.fromCode(reader.readByte());

        final int notificationId = reader.readInt();
        if (command == GET_NOTIFICATION_ATTRIBUTES) {
            final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap = createGetNotificationAttributesCommand(reader);
            return new NotificationControlMessage(garminMessage, command, notificationId, notificationAttributesMap);
        } else if (command == PERFORM_LEGACY_NOTIFICATION_ACTION) {
            NotificationsHandler.LegacyNotificationAction[] values = NotificationsHandler.LegacyNotificationAction.values();
            final NotificationsHandler.LegacyNotificationAction legacyNotificationAction = values[reader.readByte()];
            return new NotificationControlMessage(garminMessage, command, notificationId, legacyNotificationAction);
        } else if (command == PERFORM_NOTIFICATION_ACTION) {
            final int actionId = reader.readByte();
            final NotificationsHandler.NotificationAction notificationAction = NotificationsHandler.NotificationAction.fromCode(actionId);
            final String actionString = reader.readNullTerminatedString();
            return new NotificationControlMessage(garminMessage, command, notificationId, notificationAction, actionString);
        }
        LOG.warn("Unknown NotificationCommand in NotificationControlMessage");

        return null;
    }

    private static Map<NotificationsHandler.NotificationAttribute, Integer> createGetNotificationAttributesCommand(MessageReader reader) {
        final Map<NotificationsHandler.NotificationAttribute, Integer> notificationAttributesMap = new LinkedHashMap<>();
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
                maxLength = reader.readShort(); //TODO this is wrong
                // TODO: What is this??
                reader.readByte();

            } else {
                maxLength = 0;
            }
            notificationAttributesMap.put(attribute, maxLength);
        }
        return notificationAttributesMap;
    }

    public String getActionString() {
        return actionString;
    }

    public void addGbDeviceEvent(GBDeviceEvent gbDeviceEvent) {
        if (null == this.gbDeviceEventList)
            this.gbDeviceEventList = new ArrayList<>();
        this.gbDeviceEventList.add(gbDeviceEvent);
    }

    @Override
    public List<GBDeviceEvent> getGBDeviceEvent() {
        if (null == this.gbDeviceEventList)
            return Collections.emptyList();
        return gbDeviceEventList;
    }

    public NotificationsHandler.LegacyNotificationAction getLegacyNotificationAction() {
        return legacyNotificationAction;
    }

    public NotificationsHandler.NotificationAction getNotificationAction() {
        return notificationAction;
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
