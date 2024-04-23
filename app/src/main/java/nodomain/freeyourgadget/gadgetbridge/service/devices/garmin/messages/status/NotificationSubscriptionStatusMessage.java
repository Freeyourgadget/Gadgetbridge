package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class NotificationSubscriptionStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final NotificationStatus notificationStatus;
    private final int enableRaw;
    private final int unk;
    private final boolean sendOutgoing;

    public NotificationSubscriptionStatusMessage(Status status, NotificationStatus notificationStatus, boolean enable, int unk) {
        this.garminMessage = GarminMessage.NOTIFICATION_SUBSCRIPTION;
        this.status = status;
        this.notificationStatus = notificationStatus;
        this.enableRaw = enable ? 1 : 0;
        this.unk = unk;
        this.sendOutgoing = true;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(garminMessage.getId());
        writer.writeByte(status.ordinal());
        writer.writeByte(notificationStatus.ordinal());
        writer.writeByte(this.enableRaw);
        writer.writeByte(this.unk);

        return this.sendOutgoing;
    }

    public enum NotificationStatus {
        ENABLED,
        DISABLED
        ;

        public static NotificationStatus fromId(int id) {
            for (NotificationStatus notificationStatus :
                    NotificationStatus.values()) {
                if (notificationStatus.ordinal() == id) {
                    return notificationStatus;
                }
            }
            return null;
        }
    }
}
