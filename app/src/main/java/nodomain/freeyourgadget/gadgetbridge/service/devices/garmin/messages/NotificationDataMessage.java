package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

public class NotificationDataMessage extends GFDIMessage {
    private final byte[] chunk;
    private final int dataOffset;
    private final int messageSize;
    private final boolean sendOutgoing;
    private final int crc;

    public NotificationDataMessage(byte[] chunk, int messageSize, int dataOffset, int crc) {
        this(chunk, messageSize, dataOffset, crc, true);
    }

    public NotificationDataMessage(byte[] chunk, int messageSize, int dataOffset, int crc, boolean sendOutgoing) {
        this.garminMessage = GarminMessage.NOTIFICATION_DATA;
        this.dataOffset = dataOffset;
        this.crc = crc;
        this.chunk = chunk;
        this.messageSize = messageSize;

        this.sendOutgoing = sendOutgoing;
    }

    public int getCrc() {
        return crc;
    }

    @Override
    protected boolean generateOutgoing() {
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(garminMessage.getId());
        writer.writeShort(messageSize);
        writer.writeShort(crc);
        writer.writeShort(dataOffset);
        writer.writeBytes(chunk);
        return sendOutgoing;
    }
}
