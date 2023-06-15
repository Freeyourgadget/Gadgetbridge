package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

public class AncsPerformNotificationAction extends AncsControlCommand {
    public final int notificationUID;
    public final AncsAction action;

    public AncsPerformNotificationAction(int notificationUID, AncsAction action) {
        super(AncsCommand.PERFORM_NOTIFICATION_ACTION);
        this.notificationUID = notificationUID;
        this.action = action;
    }
}
