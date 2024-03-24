package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.Instant;
import org.threeten.bp.ZoneId;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionDayOfWeek extends FieldDefinition {

    public FieldDefinitionDayOfWeek(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }

    @Override
    public Object decode(ByteBuffer byteBuffer) {
        int raw = (int) baseType.decode(byteBuffer, scale, offset);
        return DayOfWeek.of(raw == 0 ? 7 : raw);
    }

    @Override
    public void encode(ByteBuffer byteBuffer, Object o) {
        if (o instanceof DayOfWeek) {
            baseType.encode(byteBuffer, (((DayOfWeek) o).getValue() % 7), scale, offset);
            return;
        }
        baseType.encode(byteBuffer, (Instant.ofEpochSecond((int) o).atZone(ZoneId.systemDefault()).getDayOfWeek().getValue() % 7), scale, offset);
    }
}
