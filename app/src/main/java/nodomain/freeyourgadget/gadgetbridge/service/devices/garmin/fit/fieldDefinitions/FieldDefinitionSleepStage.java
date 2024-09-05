package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionSleepStage extends FieldDefinition {
    public FieldDefinitionSleepStage(final int localNumber, final int size, final BaseType baseType, final String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(final ByteBuffer byteBuffer) {
        final int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return SleepStage.fromId(raw);
    }

    @Override
    public void encode(final ByteBuffer byteBuffer, final Object o) {
        if (o instanceof SleepStage) {
            baseType.encode(byteBuffer, (((SleepStage) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    public enum SleepStage {
        UNMEASURABLE(0),
        AWAKE(1),
        LIGHT(2),
        DEEP(3),
        REM(4),
        ;

        private final int id;

        SleepStage(final int i) {
            id = i;
        }

        @Nullable
        public static SleepStage fromId(final int id) {
            for (SleepStage stage : SleepStage.values()) {
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
