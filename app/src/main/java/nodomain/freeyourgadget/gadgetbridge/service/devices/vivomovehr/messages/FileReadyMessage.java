package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class FileReadyMessage {
    public static final int TRIGGER_MANUAL = 0;
    public static final int TRIGGER_AUTOMATIC = 1;

    public final int fileIndex;
    public final int dataType;
    public final int fileSubtype;
    public final int fileNumber;
    public final int specificFileFlags;
    public final int generalFileFlags;
    public final int fileSize;
    public final int fileDate;
    public final int triggerMethod;

    public FileReadyMessage(int fileIndex, int dataType, int fileSubtype, int fileNumber, int specificFileFlags, int generalFileFlags, int fileSize, int fileDate, int triggerMethod) {
        this.fileIndex = fileIndex;
        this.dataType = dataType;
        this.fileSubtype = fileSubtype;
        this.fileNumber = fileNumber;
        this.specificFileFlags = specificFileFlags;
        this.generalFileFlags = generalFileFlags;
        this.fileSize = fileSize;
        this.fileDate = fileDate;
        this.triggerMethod = triggerMethod;
    }

    public static FileReadyMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);

        final int fileIndex = reader.readShort();
        final int dataType = reader.readByte();
        final int fileSubtype = reader.readByte();
        final int fileNumber = reader.readShort();
        final int specificFileFlags = reader.readByte();
        final int generalFileFlags = reader.readByte();
        final int fileSize = reader.readInt();
        final int fileDate = reader.readInt();
        final int triggerMethod = reader.readByte();

        return new FileReadyMessage(fileIndex, dataType, fileSubtype, fileNumber, specificFileFlags, generalFileFlags, fileSize, fileDate, triggerMethod);
    }
}
