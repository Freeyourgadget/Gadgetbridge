package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.fieldDefinitions;

import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.FieldDefinition;
import nodomain.freeyourgadget.gadgetbridge.service.devices.garmin.fit.baseTypes.BaseType;

public class FieldDefinitionHrZoneHighBoundary extends FieldDefinition {
    public FieldDefinitionHrZoneHighBoundary(final int localNumber, final int size, final BaseType baseType, final String name) {
        super(localNumber, size, baseType, name, 1, 0);
    }
}
