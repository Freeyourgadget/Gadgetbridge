package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import androidx.annotation.Nullable;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionLanguage extends FieldDefinition {

    public FieldDefinitionLanguage(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return Language.fromId(raw);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof Language) {
            baseType.encode(byteBuffer, (((Language) o).getId()), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, o, scale, offset);
    }

    private enum Language {
        english(0),
        italian(2),
        ;

        private final int id;

        Language(int i) {
            id = i;
        }

        @Nullable
        public static Language fromId(int id) {
            for (Language language :
                    Language.values()) {
                if (id == language.getId()) {
                    return language;
                }
            }
            return null;
        }

        public int getId() {
            return id;
        }
    }
}
