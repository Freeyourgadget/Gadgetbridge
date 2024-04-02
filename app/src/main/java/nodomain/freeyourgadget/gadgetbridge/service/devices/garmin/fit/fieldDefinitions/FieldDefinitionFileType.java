package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionFileType extends FieldDefinition {

    public FieldDefinitionFileType(int localNumber, int size, BaseType baseType, String name) {
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
            baseType.encode(byteBuffer, (((Type) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum Type {
        settings(2),
        activity(4), //FIT_TYPE_4 stands for activity directory
        goals(11),
        monitor(32), //FIT_TYPE_32
        changelog(41), // FIT_TYPE_41 stands for changelog directory
        metrics(44), //FIT_TYPE_41
        sleep(49), //FIT_TYPE_49
        ;

        private final int id;

        Type(int i) {
            this.id = i;
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
            return this.id;
        }
    }
}
