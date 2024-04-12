package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages;

import java.util.Random;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.FileType;

public class CreateFileMessage extends GFDIMessage {

    private final int fileSize;
    private final FileType.FILETYPE filetype;
    private final boolean generateOutgoing;

    public CreateFileMessage(GarminMessage garminMessage, int fileSize, FileType.FILETYPE filetype) {
        this.fileSize = fileSize;
        this.filetype = filetype;
        this.garminMessage = garminMessage;
        this.statusMessage = this.getStatusMessage();
        this.generateOutgoing = false;
    }

    public CreateFileMessage(int fileSize, FileType.FILETYPE filetype) {
        this.garminMessage = GarminMessage.CREATE_FILE;
        this.fileSize = fileSize;
        this.filetype = filetype;
        this.statusMessage = this.getStatusMessage();
        this.generateOutgoing = true;
    }

    public static CreateFileMessage parseIncoming(MessageReader reader, GarminMessage garminMessage) {

        final int fileSize = reader.readInt();
        final int dataType = reader.readByte(); //SupportedFileTypesStatusMessage.FileTypeInfo.type
        final int subType = reader.readByte();//SupportedFileTypesStatusMessage.FileTypeInfo.subtypetype
        final FileType.FILETYPE filetype = FileType.FILETYPE.fromDataTypeSubType(dataType, subType);
        final int fileIndex = reader.readShort(); //???
        reader.readByte(); //unk
        final int subTypeMask = reader.readByte(); //???
        final int numberMask = reader.readShort(); //???

        return new CreateFileMessage(garminMessage, fileSize, filetype);
    }

    @Override
    protected boolean generateOutgoing() { //TODO: adjust variables
        Random random = new Random();
        final MessageWriter writer = new MessageWriter(response);
        writer.writeShort(0); // packet size will be filled below
        writer.writeShort(this.garminMessage.getId());
        writer.writeInt(this.fileSize);
        writer.writeByte(this.filetype.getType());
        writer.writeByte(this.filetype.getSubType());
        writer.writeShort(0); //fileIndex
        writer.writeByte(0); //reserved
        writer.writeByte(0); //subtypemask
        writer.writeShort(65535); //numbermask
        writer.writeShort(0); ///???
        writer.writeLong(random.nextLong());

        return generateOutgoing;
    }

}
