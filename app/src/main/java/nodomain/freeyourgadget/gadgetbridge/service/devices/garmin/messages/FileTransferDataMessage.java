package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.status.FileTransferDataStatusMessage;

public class FileTransferDataMessage extends GFDIMessage {

    private final byte[] message;
    private final int dataOffset;
    private final boolean sendOutgoing;
    private final int crc;

    public FileTransferDataMessage(byte[] message, int dataOffset, int crc) {
        this(message, dataOffset, crc, true);
    }

    public FileTransferDataMessage(byte[] message, int dataOffset, int crc, boolean sendOutgoing) {
        this.garminMessage = GarminMessage.FILE_TRANSFER_DATA;
        this.dataOffset = dataOffset;
        this.crc = crc;
        this.message = message;

        this.statusMessage = new FileTransferDataStatusMessage(GarminMessage.FILE_TRANSFER_DATA, Status.ACK, FileTransferDataStatusMessage.TransferStatus.OK, dataOffset);
        this.sendOutgoing = sendOutgoing;
    }

    public static FileTransferDataMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {

        final int flags = reader.readByte();
        final int crc = reader.readShort();
        final int dataOffset = reader.readInt();
        final byte[] message = reader.readBytes(reader.remaining());

        return new FileTransferDataMessage(message, dataOffset, crc, false);
    }

    public byte[] getMessage() {
        return message;
    }

    public int getCrc() {
        return crc;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(garminMessage.getId());
        writer.writeByte(0); //flags?
        writer.writeShort(crc);
        writer.writeInt(dataOffset);
        writer.writeBytes(message);
        return sendOutgoing;
    }

}
