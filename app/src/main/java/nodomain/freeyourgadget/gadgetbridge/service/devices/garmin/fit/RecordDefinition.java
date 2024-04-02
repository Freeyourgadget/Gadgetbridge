package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class RecordDefinition {
    private final RecordHeader recordHeader;
    private final int globalMesgNum;
    private final java.nio.ByteOrder byteOrder;
    private final MesgType mesgType;
    private List<FieldDefinition> fieldDefinitions;
    private List<DevFieldDefinition> devFieldDefinitions;


    public RecordDefinition(RecordHeader recordHeader, ByteOrder byteOrder, MesgType mesgType, int globalMesgNum, List<FieldDefinition> fieldDefinitions, List<DevFieldDefinition> devFieldDefinitions) {
        this.recordHeader = recordHeader;
        this.byteOrder = byteOrder;
        this.mesgType = mesgType;
        this.globalMesgNum = globalMesgNum;
        this.fieldDefinitions = fieldDefinitions;
        this.devFieldDefinitions = devFieldDefinitions;
    }

    public RecordDefinition(RecordHeader recordHeader, ByteOrder byteOrder, MesgType mesgType, List<FieldDefinition> fieldDefinitions) {
        this(recordHeader, byteOrder, mesgType, mesgType.getGlobalMesgNum(), fieldDefinitions, null);
    }

    public RecordDefinition(RecordHeader recordHeader, ByteOrder byteOrder, MesgType mesgType, int globalMesgNum) {
        this(recordHeader, byteOrder, mesgType, globalMesgNum, null, null);
    }

    public static RecordDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader, RecordHeader recordHeader) {
        if (!recordHeader.isDefinition())
            return null;
        garminByteBufferReader.readByte();//ignore
        ByteOrder byteOrder = garminByteBufferReader.readByte() == 0x01 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        garminByteBufferReader.setByteOrder(byteOrder);
        final int globalMesgNum = garminByteBufferReader.readShort();

        RecordDefinition definitionMessage = new RecordDefinition(recordHeader, byteOrder, recordHeader.getMesgType(), globalMesgNum);

        final int numFields = garminByteBufferReader.readByte();
        List<FieldDefinition> fieldDefinitions = new ArrayList<>(numFields);
        for (int i = 0; i < numFields; i++) {
            fieldDefinitions.add(FieldDefinition.parseIncoming(garminByteBufferReader));
        }

        definitionMessage.setFieldDefinitions(fieldDefinitions);

        if (recordHeader.isDeveloperData()) {
            final int numDevFields = garminByteBufferReader.readByte();
            List<DevFieldDefinition> devFieldDefinitions = new ArrayList<>(numDevFields);
            for (int i = 0; i < numDevFields; i++) {
                devFieldDefinitions.add(DevFieldDefinition.parseIncoming(garminByteBufferReader));
            }
            definitionMessage.setDevFieldDefinitions(devFieldDefinitions);
        }

        return definitionMessage;
    }

    public ByteOrder getByteOrder() {
        return byteOrder;
    }

    public List<DevFieldDefinition> getDevFieldDefinitions() {
        return devFieldDefinitions;
    }

    public void setDevFieldDefinitions(List<DevFieldDefinition> devFieldDefinitions) {
        this.devFieldDefinitions = devFieldDefinitions;
    }

    public RecordHeader getRecordHeader() {
        return recordHeader;
    }

    public List<FieldDefinition> getFieldDefinitions() {
        return fieldDefinitions;
    }

    public void setFieldDefinitions(List<FieldDefinition> fieldDefinitions) {
        this.fieldDefinitions = fieldDefinitions;
    }

    public void generateOutgoingPayload(MessageWriter writer) {
        writer.writeByte(recordHeader.generateOutgoingDefinitionPayload());
        writer.writeByte(0);//ignore
        writer.writeByte(byteOrder == ByteOrder.LITTLE_ENDIAN ? 0 : 1);
        writer.setByteOrder(byteOrder);
        writer.writeShort(globalMesgNum);
        writer.writeByte(fieldDefinitions.size());
        for (FieldDefinition fieldDefinition : fieldDefinitions) {
            fieldDefinition.generateOutgoingPayload(writer);
        }
    }

    public String getName() {
        return mesgType != null ? mesgType.name() : "unknown_" + globalMesgNum;
    }

}
