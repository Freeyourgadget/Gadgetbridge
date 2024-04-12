package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.apache.commons.lang3.EnumUtils;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class NotificationUpdateMessage extends GFDIMessage {

    final private NotificationUpdateType notificationUpdateType;
    final private NotificationType notificationType;
    final private int count; //how many notifications of the same type are present
    final private int notificationId;

    public NotificationUpdateMessage(NotificationUpdateType notificationUpdateType, NotificationType notificationType, int count, int notificationId) {
        this.garminMessage = GarminMessage.NOTIFICATION_UPDATE;
        this.notificationUpdateType = notificationUpdateType;
        this.notificationType = notificationType;
        this.count = count;
        this.notificationId = notificationId;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeByte(this.notificationUpdateType.ordinal());
        writer.writeByte(getCategoryFlags(this.notificationType));
        writer.writeByte(getCategoryValue(this.notificationType));
        writer.writeByte(this.count);
        writer.writeInt(this.notificationId);
        writer.writeByte(0); //unk (extra flags)

        return true;
    }

    private int getCategoryFlags(NotificationType notificationType) {
        switch (notificationType.getGenericType()) {
            case "generic_phone":
            case "generic_email":
            case "generic_sms":
            case "generic_chat":
                return (int) EnumUtils.generateBitVector(NotificationFlag.class, NotificationFlag.FOREGROUND);
            case "generic_navigation":
            case "generic_social":
            case "generic_alarm_clock":
            case "generic":
                return (int) EnumUtils.generateBitVector(NotificationFlag.class, NotificationFlag.BACKGROUND);
        }
        return 1;
    }

    private int getCategoryValue(NotificationType notificationType) {
        switch (notificationType.getGenericType()) {
            case "generic_phone":
                return NotificationCategory.INCOMING_CALL.ordinal();
            case "generic_email":
                return NotificationCategory.EMAIL.ordinal();
            case "generic_sms":
            case "generic_chat":
                return NotificationCategory.SMS.ordinal();
            case "generic_navigation":
                return NotificationCategory.LOCATION.ordinal();
            case "generic_social":
                return NotificationCategory.SOCIAL.ordinal();
            case "generic_alarm_clock":
            case "generic":
                return NotificationCategory.OTHER.ordinal();
        }
        return NotificationCategory.OTHER.ordinal();
    }

    public enum NotificationUpdateType {
        ADD,
        MODIFY,
        REMOVE,
    }

    enum NotificationFlag { //was AncsEventFlag
        BACKGROUND,
        FOREGROUND,
    }

    enum NotificationCategory { //was AncsCategory
        OTHER,
        INCOMING_CALL,
        MISSED_CALL,
        VOICEMAIL,
        SOCIAL,
        SCHEDULE,
        EMAIL,
        NEWS,
        HEALTH_AND_FITNESS,
        BUSINESS_AND_FINANCE,
        LOCATION,
        ENTERTAINMENT,
        SMS
    }
}
