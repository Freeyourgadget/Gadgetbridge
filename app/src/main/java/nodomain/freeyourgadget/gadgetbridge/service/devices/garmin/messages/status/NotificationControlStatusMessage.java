package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class NotificationControlStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final NotificationChunkStatus notificationChunkStatus;
    private final NotificationStatusCode notificationStatusCode;
    private final boolean sendOutgoing;

    public NotificationControlStatusMessage(GarminMessage garminMessage, Status status, NotificationChunkStatus notificationChunkStatus, NotificationStatusCode notificationStatusCode) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.notificationChunkStatus = notificationChunkStatus;
        this.notificationStatusCode = notificationStatusCode;
        this.sendOutgoing = true;
    }


    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(garminMessage.getId());
        writer.writeByte(status.ordinal());
        writer.writeByte(notificationChunkStatus.ordinal());
        writer.writeByte(notificationStatusCode.getCode());

        return this.sendOutgoing;
    }

    public enum NotificationChunkStatus {
        OK,
        ERROR,
        ;

        public static NotificationChunkStatus fromId(int id) {
            for (NotificationChunkStatus notificationChunkStatus :
                    NotificationChunkStatus.values()) {
                if (notificationChunkStatus.ordinal() == id) {
                    return notificationChunkStatus;
                }
            }
            return null;
        }
    }

    public enum NotificationStatusCode {
        NO_ERROR(0),
        UNKNOWN_COMMAND(160),
        ;

        private final int code;

        NotificationStatusCode(final int code) {
            this.code = code;
        }

        @Nullable
        public static NotificationStatusCode fromCode(final int code) {
            for (final NotificationStatusCode notificationStatusCode : NotificationStatusCode.values()) {
                if (notificationStatusCode.getCode() == code) {
                    return notificationStatusCode;
                }
            }
            return null;
        }

        public int getCode() {
            return code;
        }
    }
}
