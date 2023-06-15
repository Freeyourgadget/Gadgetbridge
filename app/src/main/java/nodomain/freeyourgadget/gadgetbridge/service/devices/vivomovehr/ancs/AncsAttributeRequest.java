package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

public class AncsAttributeRequest {
    public final AncsAttribute attribute;
    public final int maxLength;

    public AncsAttributeRequest(AncsAttribute attribute, int maxLength) {
        this.attribute = attribute;
        this.maxLength = maxLength;
    }
}
