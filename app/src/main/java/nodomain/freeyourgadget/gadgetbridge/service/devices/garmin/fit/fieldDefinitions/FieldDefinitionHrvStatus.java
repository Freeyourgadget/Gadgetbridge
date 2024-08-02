package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionHrvStatus extends FieldDefinition {
    public FieldDefinitionHrvStatus(final int localNumber, final int size, final BaseType baseType, final String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer) {
        final int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return HrvStatus.fromId(raw);
    }

    @Override
    public void encode(final ByteBuffer byteBuffer, final Object o) {
        if (o instanceof HrvStatus) {
            baseType.encode(byteBuffer, (((HrvStatus) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum HrvStatus {
        NONE(0),
        POOR(1),
        LOW(2),
        UNBALANCED(3),
        BALANCED(4),
        ;

        private final int id;

        HrvStatus(final int i) {
            id = i;
        }

        @Nullable
        public static HrvStatus fromId(final int id) {
            for (HrvStatus stage : HrvStatus.values()) {
                if (id == stage.getId()) {
                    return stage;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }
}
