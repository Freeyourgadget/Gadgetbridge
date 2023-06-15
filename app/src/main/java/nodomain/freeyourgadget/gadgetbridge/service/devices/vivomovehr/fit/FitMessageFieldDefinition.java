package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages.MessageWriter;

public class FitMessageFieldDefinition {
    public final String fieldName;
    public final int fieldNumber;
    public final int fieldSize;
    public final FitFieldBaseType fieldType;
    public final double scale;
    public final double offset;
    public final String units;
    public final Object defaultValue;

    public FitMessageFieldDefinition(String fieldName, int fieldNumber, int fieldSize, FitFieldBaseType fieldType, Object defaultValue) {
        this(fieldName, fieldNumber, fieldSize, fieldType, 0, 0, null, defaultValue);
    }

    public FitMessageFieldDefinition(String fieldName, int fieldNumber, int fieldSize, FitFieldBaseType fieldType, double scale, double offset, String units, Object defaultValue) {
        this.fieldName = fieldName;
        this.fieldNumber = fieldNumber;
        this.fieldSize = fieldSize;
        this.fieldType = fieldType;
        this.scale = scale;
        this.offset = offset;
        this.units = units;
        this.defaultValue = defaultValue == null ? fieldType.invalidValue : defaultValue;
    }

    public void writeToMessage(MessageWriter writer) {
        writer.writeByte(fieldNumber);
        writer.writeByte(fieldSize);
        writer.writeByte(fieldType.typeID);
    }
}
