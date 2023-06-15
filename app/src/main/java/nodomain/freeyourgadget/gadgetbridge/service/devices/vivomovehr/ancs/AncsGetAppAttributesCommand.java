package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import java.util.List;

public class AncsGetAppAttributesCommand extends AncsControlCommand {
    public final String appIdentifier;
    public final List<AncsAppAttribute> requestedAttributes;

    public AncsGetAppAttributesCommand(String appIdentifier, List<AncsAppAttribute> requestedAttributes) {
        super(AncsCommand.GET_APP_ATTRIBUTES);
        this.appIdentifier = appIdentifier;
        this.requestedAttributes = requestedAttributes;
    }
}
