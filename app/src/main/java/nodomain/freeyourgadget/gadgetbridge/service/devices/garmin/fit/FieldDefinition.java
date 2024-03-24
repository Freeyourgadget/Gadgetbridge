package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageReader;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.messages.MessageWriter;

public class FieldDefinition implements FieldInterface {
    private final int localNumber;
    private final int size;
    protected final BaseType baseType;
    private final String name;
    protected final int scale;
    protected final int offset;

    public FieldDefinition(int localNumber, int size, BaseType baseType, String name, int scale, int offset) {
        this.localNumber = localNumber;
        this.size = size;
        this.baseType = baseType;
        this.name = name;
        this.scale = scale;
        this.offset = offset;
    }

    public FieldDefinition(int localNumber, int size, BaseType baseType, String name) {
        this(localNumber, size, baseType, name, 1, 0);
    }

    public static FieldDefinition parseIncoming(MessageReader reader) {
        int localNumber = reader.readByte();
        int size = reader.readByte();
        int baseTypeIdentifier = reader.readByte();

        BaseType baseType = BaseType.fromIdentifier(baseTypeIdentifier);

        if (size % baseType.getSize() != 0) {
            baseType = BaseType.BASE_TYPE_BYTE;
        }

        return new FieldDefinition(localNumber, size, baseType, "");

    }

    public int getScale() {
        return scale;
    }

    public int getOffset() {
        return offset;
    }

    public int getLocalNumber() {
        return localNumber;
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
        writer.writeByte(localNumber);
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
