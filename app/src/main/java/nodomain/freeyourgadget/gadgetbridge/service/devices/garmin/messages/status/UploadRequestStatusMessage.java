package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

public class UploadRequestStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final UploadStatus uploadStatus;
    private final int dataOffset;
    private final int maxFileSize;
    private final int crcSeed;

    public UploadRequestStatusMessage(GarminMessage garminMessage, Status status, UploadStatus uploadStatus, int dataOffset, int maxFileSize, int crcSeed) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.uploadStatus = uploadStatus;
        this.dataOffset = dataOffset;
        this.maxFileSize = maxFileSize;
        this.crcSeed = crcSeed;
    }

    public static UploadRequestStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());

        if (!status.equals(Status.ACK)) {
            return null;
        }
        final UploadStatus uploadStatus = UploadStatus.fromId(reader.readByte());
        final int dataOffset = reader.readInt();
        final int maxFileSize = reader.readInt();
        final int crcSeed = reader.readShort();

        if (!UploadStatus.OK.equals(uploadStatus)) {
            LOG.warn("Received {} / {} for message {}", status, uploadStatus, garminMessage);
        } else {
            LOG.info("Received {} / {} for message {}", status, uploadStatus, garminMessage);
        }
        return new UploadRequestStatusMessage(garminMessage, status, uploadStatus, dataOffset, maxFileSize, crcSeed);
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getCrcSeed() {
        return crcSeed;
    }

    public boolean canProceed() {
        return status.equals(Status.ACK) && uploadStatus.equals(UploadStatus.OK);
    }

    public enum UploadStatus {
        OK,
        INDEX_UNKNOWN,
        INDEX_NOT_WRITEABLE,
        NO_SPACE_LEFT,
        INVALID,
        NOT_READY,
        CRC_INCORRECT,
        ;

        public static UploadStatus fromId(int id) {
            for (UploadStatus uploadStatus :
                    UploadStatus.values()) {
                if (uploadStatus.ordinal() == id) {
                    return uploadStatus;
                }
            }
            return null;
        }
    }
}
