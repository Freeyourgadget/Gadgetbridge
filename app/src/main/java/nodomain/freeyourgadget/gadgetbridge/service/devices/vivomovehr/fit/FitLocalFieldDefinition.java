package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.fit;

class FitLocalFieldDefinition {
    public final FitMessageFieldDefinition globalDefinition;
    public final int size;
    public final FitFieldBaseType baseType;

    FitLocalFieldDefinition(FitMessageFieldDefinition globalDefinition, int size, FitFieldBaseType baseType) {
        this.globalDefinition = globalDefinition;
        this.size = size;
        this.baseType = baseType;
    }
}
