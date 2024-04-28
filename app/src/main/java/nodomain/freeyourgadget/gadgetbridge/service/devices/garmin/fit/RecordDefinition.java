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
    private final java.nio.ByteOrder byteOrder;
    private List<FieldDefinition> fieldDefinitions;
    private List<DevFieldDefinition> devFieldDefinitions;

    public RecordDefinition(RecordHeader recordHeader, ByteOrder byteOrder, GlobalFITMessage globalFITMessage, List<FieldDefinition> fieldDefinitions, List<DevFieldDefinition> devFieldDefinitions) {
        this.recordHeader = recordHeader;
        this.byteOrder = byteOrder;
        this.globalFITMessage = globalFITMessage;
        this.fieldDefinitions = fieldDefinitions;
        this.devFieldDefinitions = devFieldDefinitions;
    }

    public static RecordDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader, RecordHeader recordHeader) {
        if (!recordHeader.isDefinition())
            return null;
        garminByteBufferReader.readByte();//ignore
        ByteOrder byteOrder = garminByteBufferReader.readByte() == 0x01 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
        garminByteBufferReader.setByteOrder(byteOrder);
        final int globalMesgNum = garminByteBufferReader.readShort();
        final GlobalFITMessage globalFITMessage = GlobalFITMessage.fromNumber(globalMesgNum);

        RecordDefinition definitionMessage = new RecordDefinition(recordHeader, byteOrder, globalFITMessage, null, null);

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

    @NonNull
    public String toString() {
        return System.lineSeparator() + recordHeader.toString() +
                " Global Message Number: " + globalFITMessage.name();
    }

    public void populateDevFields(RecordData recordData) {
        for (DevFieldDefinition devFieldDef : getDevFieldDefinitions()) {
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
