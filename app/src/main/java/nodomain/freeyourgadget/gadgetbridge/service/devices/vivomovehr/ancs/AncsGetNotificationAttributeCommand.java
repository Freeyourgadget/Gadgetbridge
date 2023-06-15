package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

import java.util.List;

public class AncsGetNotificationAttributeCommand extends AncsControlCommand {
    public final int notificationUID;
    public final List<AncsAttributeRequest> attributes;

    public AncsGetNotificationAttributeCommand(int notificationUID, List<AncsAttributeRequest> attributes) {
        super(AncsCommand.GET_NOTIFICATION_ATTRIBUTES);
        this.notificationUID = notificationUID;
        this.attributes = attributes;
    }
}
