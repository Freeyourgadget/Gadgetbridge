package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class DevFieldDefinition {
    public final ByteBuffer valueHolder;
    private final int fieldDefinitionNumber;
    private final int size;
    private final int developerDataIndex;
    private BaseType baseType;
    private String name;

    public DevFieldDefinition(int fieldDefinitionNumber, int size, int developerDataIndex, String name) {
        this.fieldDefinitionNumber = fieldDefinitionNumber;
        this.size = size;
        this.developerDataIndex = developerDataIndex;
        this.name = name;
        this.valueHolder = ByteBuffer.allocate(size);
    }

    public static DevFieldDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader) {
        int number = garminByteBufferReader.readByte();
        int size = garminByteBufferReader.readByte();
        int developerDataIndex = garminByteBufferReader.readByte();

        return new DevFieldDefinition(number, size, developerDataIndex, "");

    }

    public BaseType getBaseType() {
        return baseType;
    }

    public void setBaseType(BaseType baseType) {
        this.baseType = baseType;
    }

    public int getDeveloperDataIndex() {
        return developerDataIndex;
    }

    public int getFieldDefinitionNumber() {
        return fieldDefinitionNumber;
    }

    public int getSize() {
        return size;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
