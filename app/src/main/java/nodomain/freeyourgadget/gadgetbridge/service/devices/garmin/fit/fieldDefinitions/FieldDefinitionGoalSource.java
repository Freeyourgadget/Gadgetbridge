package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionGoalSource extends FieldDefinition {

    public FieldDefinitionGoalSource(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return Source.fromId(raw);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Source) {
            baseType.encode(byteBuffer, (((Source) o).ordinal()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum Source {
        auto,
        community,
        manual,
        ;

        @Nullable
        public static Source fromId(int id) {
            for (Source source :
                    Source.values()) {
                if (id == source.ordinal()) {
                    return source;
                }
            }
            return null;
        }
    }
}
