package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class FileTransferDataStatusMessage extends GFDIStatusMessage {
    private final Status status;
    private final TransferStatus transferStatus;
    private final int dataOffset;
    private final boolean sendOutgoing;
    public FileTransferDataStatusMessage(GarminMessage garminMessage, Status status, TransferStatus transferStatus, int dataOffset) {
        this(garminMessage, status, transferStatus, dataOffset, true);
    }


    public FileTransferDataStatusMessage(GarminMessage garminMessage, Status status, TransferStatus transferStatus, int dataOffset, boolean sendOutgoing) {
        this.garminMessage = garminMessage;
        this.status = status;
        this.transferStatus = transferStatus;
        this.dataOffset = dataOffset;
        this.sendOutgoing = sendOutgoing;
    }

    public static FileTransferDataStatusMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {
        final Status status = Status.fromCode(reader.readByte());

        if (!status.equals(Status.ACK)) {
            return null;
        }
        final TransferStatus transferStatus = TransferStatus.fromId(reader.readByte());
        final int dataOffset = reader.readInt();

        if (!TransferStatus.OK.equals(transferStatus)) {
            LOG.warn("Received {} / {} for message {}", status, transferStatus, garminMessage);
        } else {
            LOG.info("Received {} / {} for message {}", status, transferStatus, garminMessage);
        }
        return new FileTransferDataStatusMessage(garminMessage, status, transferStatus, dataOffset, false);
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public boolean canProceed() {
        return status.equals(Status.ACK) && transferStatus.equals(TransferStatus.OK);
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(GarminMessage.RESPONSE.getId());
        writer.writeShort(garminMessage.getId());
        writer.writeByte(status.ordinal());
        writer.writeByte(transferStatus.ordinal());
        writer.writeInt(dataOffset);

        return sendOutgoing;
    }

    public enum TransferStatus {
        OK,
        RESEND,
        ABORT,
        CRC_MISMATCH,
        OFFSET_MISMATCH,
        SYNC_PAUSED,
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
