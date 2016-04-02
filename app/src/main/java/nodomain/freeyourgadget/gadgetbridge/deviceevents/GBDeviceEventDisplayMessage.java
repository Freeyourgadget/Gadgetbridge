package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventDisplayMessage {
    public String message;
    public int duration;
    public int severity;

    /**
     * An event for displaying a message to the user. How the message is displayed
     * is a detail of the current activity, which needs to listen to the Intent
     * GB.ACTION_DISPLAY_MESSAGE.
     * @param message
     * @param duration
     * @param severity
     */
    public GBDeviceEventDisplayMessage(String message, int duration, int severity) {
        this.message = message;
        this.duration = duration;
        this.severity = severity;
    }
}
