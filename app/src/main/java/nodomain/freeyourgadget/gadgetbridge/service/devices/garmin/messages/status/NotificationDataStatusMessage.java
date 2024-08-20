package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

public class NotificationDataStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final TransferStatus transferStatus;

    public NotificationDataStatusMessage(GarminMessage garminMessage, Status status, TransferStatus transferStatus) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.transferStatus = transferStatus;
    }

    public static NotificationDataStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());

        if (!status.equals(Status.ACK)) {
            return null;
        }
        final TransferStatus transferStatus = TransferStatus.fromId(reader.readByte());

        if (!TransferStatus.OK.equals(transferStatus)) {
            LOG.warn("Received {} / {} for message {}", status, transferStatus, garminMessage);
        } else {
            LOG.info("Received {} / {} for message {}", status, transferStatus, garminMessage);
        }
        return new NotificationDataStatusMessage(garminMessage, status, transferStatus);
    }

    public boolean canProceed() {
        return status.equals(Status.ACK) && transferStatus.equals(TransferStatus.OK);
    }

    public enum TransferStatus {
        OK,
        RESEND,
        ABORT,
        CRC_MISMATCH,
        OFFSET_MISMATCH,
        ;

        public static TransferStatus fromId(int id) {
            for (TransferStatus transferStatus :
                    TransferStatus.values()) {
                if (transferStatus.ordinal() == id) {
                    return transferStatus;
                }
            }
            return null;
        }
    }
}
