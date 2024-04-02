package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionGoalType extends FieldDefinition {

    public FieldDefinitionGoalType(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return Type.fromId(raw);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Type) {
            baseType.encode(byteBuffer, (((Type) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum Type {
        steps(4),
        ;

        private final int id;

        Type(int i) {
            id = i;
        }

        @Nullable
        public static Type fromId(int id) {
            for (Type type :
                    Type.values()) {
                if (id == type.getId()) {
                    return type;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }
}
