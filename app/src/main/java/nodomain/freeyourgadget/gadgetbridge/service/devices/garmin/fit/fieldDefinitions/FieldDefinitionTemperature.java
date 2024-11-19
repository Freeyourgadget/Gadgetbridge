package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionTemperature extends FieldDefinition {

    public FieldDefinitionTemperature(int localNumber, int size, BaseType baseType, String name) {
        // #4313 - We do a "wrong" conversion to celsius on purpose
        super(localNumber, size, baseType, name, 1, -273);
    }

}
