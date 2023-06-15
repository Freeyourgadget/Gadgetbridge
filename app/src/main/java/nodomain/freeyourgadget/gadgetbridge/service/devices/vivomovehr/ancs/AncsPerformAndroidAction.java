package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.ancs;

public class AncsPerformAndroidAction extends AncsControlCommand {
    public final int notificationUID;
    public final AncsAndroidAction action;
    public final String text;

    public AncsPerformAndroidAction(int notificationUID, AncsAndroidAction action, String text) {
        super(AncsCommand.PERFORM_ANDROID_ACTION);
        this.notificationUID = notificationUID;
        this.action = action;
        this.text = text;
    }
}
