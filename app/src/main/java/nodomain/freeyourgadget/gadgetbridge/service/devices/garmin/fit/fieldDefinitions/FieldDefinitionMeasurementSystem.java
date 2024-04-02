package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionMeasurementSystem extends FieldDefinition {


    public FieldDefinitionMeasurementSystem(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return Type.fromId(raw) == null ? raw : Type.fromId(raw);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Type) {
            baseType.encode(byteBuffer, (((Type) o).ordinal()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum Type {
        metric,
        ;

        public static Type fromId(int id) {
            for (Type type :
                    Type.values()) {
                if (type.ordinal() == id) {
                    return type;
                }
            }
            return null;
        }
    }
}
