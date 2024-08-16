package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

import static nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.GarminTimeUtils.GARMIN_TIME_EPOCH;

public class FieldDefinitionTimestamp extends FieldDefinition {
    public FieldDefinitionTimestamp(int localNumber, int size, BaseType baseType, String name) {
        super(localNumber, size, baseType, name, 1, -GARMIN_TIME_EPOCH);
    }

//    @Override
//    public Object decode(ByteBuffer byteBuffer) {
//        return new Timestamp((long) baseType.decode(byteBuffer, scale, offset) * 1000L);
//    }
//
//    @Override
//    public void encode(ByteBuffer byteBuffer, Object o) {
//        if(o instanceof Timestamp) {
//            baseType.encode(byteBuffer, (int) (((Timestamp) o).getTime() / 1000L), scale, offset);
//            return;
//        }
//        baseType.encode(byteBuffer, o, scale, offset);
//    }
}
