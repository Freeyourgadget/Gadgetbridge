package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import org.apache.commons.lang3.EnumUtils;

import java.util.EnumSet;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

public class NotificationUpdateMessage extends GFDIMessage {

    final private NotificationUpdateType notificationUpdateType;
    final private NotificationType notificationType;
    final private int count; //how many notifications of the same type are present
    final private int notificationId;
    final private boolean hasActions;
    final private boolean hasPicture;
    final private boolean useLegacyActions = false;

    public NotificationUpdateMessage(NotificationUpdateType notificationUpdateType, NotificationType notificationType, int count, int notificationId, boolean hasActions, boolean hasPicture) {
        this.garminMessage = GarminMessage.NOTIFICATION_UPDATE;
        this.notificationUpdateType = notificationUpdateType;
        this.notificationType = notificationType;
        this.count = count;
        this.notificationId = notificationId;
        this.hasActions = hasActions;
        this.hasPicture = hasPicture;
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
        writer.writeByte(getNotificationPhoneFlags());

        return true;
    }

    private int getNotificationPhoneFlags() {
        EnumSet<NotificationPhoneFlags> flags = EnumSet.noneOf(NotificationPhoneFlags.class);
        if (this.hasActions)
            flags.add(NotificationPhoneFlags.NEW_ACTIONS);
        if (this.useLegacyActions)
            flags.add(NotificationPhoneFlags.LEGACY_ACTIONS);
        if (this.hasPicture)
            flags.add(NotificationPhoneFlags.HAS_ATTACHMENTS);

        return (int) EnumUtils.generateBitVector(NotificationPhoneFlags.class, flags);

    }

    private int getCategoryFlags(NotificationType notificationType) {
        EnumSet<NotificationFlag> flags = EnumSet.noneOf(NotificationFlag.class);
        if (this.hasActions && this.useLegacyActions) { //only needed for legacy actions
            flags.add(NotificationFlag.ACTION_ACCEPT);
        }
        flags.add(NotificationFlag.ACTION_DECLINE);

        switch (notificationType.getGenericType()) {
            case "generic_phone":
            case "generic_email":
            case "generic_sms":
            case "generic_chat":
                flags.add(NotificationFlag.FOREGROUND);
                break;
            case "generic_navigation":
            case "generic_social":
            case "generic_alarm_clock":
            case "generic":
                // TODO: Maybe make this configurable, but most users expect all notifications
                // to be foreground, sending them as background was generating bug reports.
                flags.add(NotificationFlag.FOREGROUND);
        }
        return (int) EnumUtils.generateBitVector(NotificationFlag.class, flags);
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
        UNK,
        ACTION_ACCEPT, //only needed for legacy actions
        ACTION_DECLINE, //only needed for legacy actions

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

    enum NotificationPhoneFlags {
        LEGACY_ACTIONS,
        NEW_ACTIONS,
        HAS_ATTACHMENTS,
        ;
    }
}
