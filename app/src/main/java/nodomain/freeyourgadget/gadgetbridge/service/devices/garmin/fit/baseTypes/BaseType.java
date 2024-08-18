package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes;

import java.nio.ByteBuffer;

//see https://github.com/dtcooper/python-fitparse/blob/master/fitparse/records.py
public enum BaseType {
    ENUM(0x00, new BaseTypeByte(true, 0xFF)),
    SINT8(0x01, new BaseTypeByte(false, 0x7F)),
    UINT8(0x02, new BaseTypeByte(true, 0xFF)),
    SINT16(0x83, new BaseTypeShort(false, 0x7FFF)),
    UINT16(0x84, new BaseTypeShort(true, 0xFFFF)),
    SINT32(0x85, new BaseTypeInt(false, 0x7FFFFFFF)),
    UINT32(0x86, new BaseTypeInt(true, 0xFFFFFFFFL)),
    STRING(0x07, new BaseTypeByte(true, 0x00)),
    FLOAT32(0x88, new BaseTypeFloat()),
    FLOAT64(0x89, new BaseTypeDouble()),
    UINT8Z(0x0A, new BaseTypeByte(true, 0x00)),
    UINT16Z(0x8B, new BaseTypeShort(true, 0)),
    UINT32Z(0x8C, new BaseTypeInt(true, 0)),
    BASE_TYPE_BYTE(0x0D, new BaseTypeByte(true, 0xFF)),
    SINT64(0x8E, new BaseTypeLong(false, 0x7FFFFFFFFFFFFFFFL)),
    UINT64(0x8F, new BaseTypeLong(true, 0xFFFFFFFFFFFFFFFFL)),
    UINT64Z(0x8F, new BaseTypeLong(true, 0)),
    ;

    private final int identifier;
    private final BaseTypeInterface baseTypeInterface;

    BaseType(int identifier, BaseTypeInterface byteBaseType) {
        this.identifier = identifier;
        this.baseTypeInterface = byteBaseType;
    }

    public static BaseType fromIdentifier(int identifier) {
        for (final BaseType baseType : BaseType.values()) {
            if (baseType.getIdentifier() == identifier) {
                return baseType;
            }
        }
        throw new IllegalArgumentException("Unknown type " + identifier);
    }

    public int getSize() {
        return baseTypeInterface.getByteSize();
    }

    public int getIdentifier() {
        return identifier;
    }

    public Object decode(ByteBuffer byteBuffer, double scale, int offset) {
        Object raw = baseTypeInterface.decode(byteBuffer, scale, offset);
        if (raw == null)
            return null;

        //the following returns STRING basetype but also all specialized FieldDefinition classes
        if (!Number.class.isAssignableFrom(raw.getClass()))
            return raw;

        switch (this) {
            case ENUM:
            case SINT8:
            case UINT8:
            case SINT16:
            case UINT16:
            case UINT8Z:
            case UINT16Z:
            case BASE_TYPE_BYTE:
                if (scale != 1) {
                    return ((Number) raw).floatValue();
                } else {
                    return ((Number) raw).intValue();
                }
            case SINT32:
            case UINT32:
            case UINT32Z:
            case SINT64:
            case UINT64:
            case UINT64Z:
                if (scale != 1) {
                    return ((Number) raw).doubleValue();
                } else {
                    return ((Number) raw).longValue();
                }
            case FLOAT32:
                return ((Number) raw).floatValue();
            case FLOAT64:
                return ((Number) raw).doubleValue();
        }
        return raw;
    }

    public void encode(ByteBuffer byteBuffer, Object o, double scale, int offset) {
        baseTypeInterface.encode(byteBuffer, o, scale, offset);
    }

    public void invalidate(ByteBuffer byteBuffer) {
        baseTypeInterface.invalidate(byteBuffer);
    }
}
