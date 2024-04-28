package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminByteBufferReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions.FieldDefinitionTimestamp;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class FieldDefinition implements FieldInterface {
    protected static final Logger LOG = LoggerFactory.getLogger(FieldDefinition.class);

    protected final BaseType baseType;
    protected final int scale;
    protected final int offset;
    private final int number;
    private final int size;
    private final String name;

    public FieldDefinition(int number, int size, BaseType baseType, String name, int scale, int offset) {
        this.number = number;
        this.size = size;
        this.baseType = baseType;
        this.name = name;
        this.scale = scale;
        this.offset = offset;
    }

    public FieldDefinition(int number, int size, BaseType baseType, String name) {
        this(number, size, baseType, name, 1, 0);
    }

    public static FieldDefinition parseIncoming(GarminByteBufferReader garminByteBufferReader, GlobalFITMessage globalFITMessage) {
        int number = garminByteBufferReader.readByte();
        int size = garminByteBufferReader.readByte();
        int baseTypeIdentifier = garminByteBufferReader.readByte();
        BaseType baseType = BaseType.fromIdentifier(baseTypeIdentifier);
        FieldDefinition global = globalFITMessage.getFieldDefinition(number, size);
        if (global != null) {
            if (global.getBaseType().equals(baseType)) {
                return global;
            } else {
                LOG.warn("Global is of type {}, but message declares {}", global.getBaseType(), baseType);
            }
        }

        if (number == 253 && size == 4 && baseType.equals(BaseType.UINT32)) {
            return new FieldDefinitionTimestamp(number, size, baseType, "253_timestamp");
        }

        return new FieldDefinition(number, size, baseType, "");
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public BaseType getBaseType() {
        return baseType;
    }

    public String getName() {
        return name;
    }

    public void generateOutgoingPayload(MessageWriter writer) {
        writer.writeByte(number);
        writer.writeByte(size);
        writer.writeByte(baseType.getIdentifier());
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        return baseType.decode(byteBuffer, scale, offset);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        baseType.encode(byteBuffer, o, scale, offset);
    }

    @Override
    public void invalidate(ByteBuffer byteBuffer) {
        baseType.invalidate(byteBuffer);
    }
}
