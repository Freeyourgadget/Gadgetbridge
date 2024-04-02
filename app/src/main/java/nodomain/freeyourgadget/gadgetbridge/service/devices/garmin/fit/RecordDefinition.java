package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class RecordDefinition {
    private final RecordHeader recordHeader;
    private final GlobalFITMessage globalFITMessage;
    private final LocalMessage localMessage;
    private final java.nio.ByteOrder byteOrder;
    private List<FieldDefinition> fieldDefinitions;
    private List<DevFieldDefinition> devFieldDefinitions;

    public RecordDefinition(RecordHeader recordHeader, ByteOrder byteOrder, LocalMessage localMessage, GlobalFITMessage globalFITMessage, List<FieldDefinition> fieldDefinitions, List<DevFieldDefinition> devFieldDefinitions) {
        this.recordHeader = recordHeader;
        this.byteOrder = byteOrder;
        this.localMessage = localMessage;
        this.globalFITMessage = globalFITMessage;
        this.fieldDefinitions = fieldDefinitions;
        this.devFieldDefinitions = devFieldDefinitions;
    }

    public RecordDefinition(ByteOrder byteOrder, LocalMessage localMessage, List<FieldDefinition> fieldDefinitions) {
        this(new RecordHeader(true, false, localMessage, null), byteOrder, localMessage, localMessage.getGlobalFITMessage(), fieldDefinitions, null);
    }

    public RecordDefinition(ByteOrder byteOrder, LocalMessage localMessage) {
        this(new RecordHeader(true, false, localMessage, null), byteOrder, localMessage, localMessage.getGlobalFITMessage(), localMessage.getLocalFieldDefinitions(), null);
    }

    public RecordDefinition(ByteOrder byteOrder, RecordHeader recordHeader, GlobalFITMessage globalFITMessage, List<FieldDefinition> fieldDefinitions) {
        this(recordHeader, byteOrder, null, globalFITMessage, fieldDefinitions, null);
    }

    public static RecordDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader, RecordHeader recordHeader) {
        if (!recordHeader.isDefinition())
            return null;
        garminByteBufferReader.readByte();//ignore
        ByteOrder byteOrder = garminByteBufferReader.readByte() == 0x01 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        garminByteBufferReader.setByteOrder(byteOrder);
        final int globalMesgNum = garminByteBufferReader.readShort();
        final GlobalFITMessage globalFITMessage = GlobalFITMessage.fromNumber(globalMesgNum);

        RecordDefinition definitionMessage = new RecordDefinition(byteOrder, recordHeader, globalFITMessage, null);

        final int numFields = garminByteBufferReader.readByte();
        List<FieldDefinition> fieldDefinitions = new ArrayList<>(numFields);

        for (int i = 0; i < numFields; i++) {
            fieldDefinitions.add(FieldDefinition.parseIncoming(garminByteBufferReader, globalFITMessage));
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

    public GlobalFITMessage getGlobalFITMessage() {
        return globalFITMessage;
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

    @Nullable
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
        writer.writeShort(globalFITMessage.getNumber());

        if (fieldDefinitions != null) {
            writer.writeByte(fieldDefinitions.size());
            for (FieldDefinition fieldDefinition : fieldDefinitions) {
                fieldDefinition.generateOutgoingPayload(writer);
            }
        }
    }

    public String getName() {
        return localMessage != null ? localMessage.name() : "unknown_" + globalFITMessage;
    }

    @NonNull
    public String toString() {
        return recordHeader.toString() +
                " Global Message Number: " + globalFITMessage.name();
    }

    public void populateDevFields(List<RecordData> developerFieldData) {
        for (DevFieldDefinition devFieldDef :
                getDevFieldDefinitions()) {
            for (RecordData recordData :
                    developerFieldData) {
                try {
                    if (devFieldDef.getFieldDefinitionNumber() == (int) recordData.getFieldByName("field_definition_number") &&
                            devFieldDef.getDeveloperDataIndex() == (int) recordData.getFieldByName("developer_data_index")) {
                        BaseType baseType = BaseType.fromIdentifier((int) recordData.getFieldByName("fit_base_type_id"));
                        devFieldDef.setBaseType(baseType);
                        devFieldDef.setName((String) recordData.getFieldByName("field_name"));
                    }
                } catch (Exception e) {
                    //ignore
                }
            }
        }

    }
}
