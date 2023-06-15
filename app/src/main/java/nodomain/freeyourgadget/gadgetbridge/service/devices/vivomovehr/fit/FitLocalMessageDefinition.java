package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

import java.util.List;

class FitLocalMessageDefinition {
    public final FitMessageDefinition globalDefinition;
    public final List<FitLocalFieldDefinition> fieldDefinitions;

    FitLocalMessageDefinition(FitMessageDefinition globalDefinition, List<FitLocalFieldDefinition> fieldDefinitions) {
        this.globalDefinition = globalDefinition;
        this.fieldDefinitions = fieldDefinitions;
    }
}
