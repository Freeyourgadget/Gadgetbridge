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
    UINT32(0x86, new BaseTypeInt(true, 0xFFFFFFFF)),
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
        for (final BaseType status : BaseType.values()) {
            if (status.getIdentifier() == identifier) {
                return status;
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

    public Object decode(ByteBuffer byteBuffer, int scale, int offset) {
        return baseTypeInterface.decode(byteBuffer, scale, offset);
    }

    public void encode(ByteBuffer byteBuffer, Object o, int scale, int offset) {
        baseTypeInterface.encode(byteBuffer, o, scale, offset);
    }

    public void invalidate(ByteBuffer byteBuffer) {
        baseTypeInterface.invalidate(byteBuffer);
    }
}
