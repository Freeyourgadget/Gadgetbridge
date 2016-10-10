package nodomain.freeyourgadget.gadgetbridge.model;

public enum NotificationType {

    UNKNOWN,

    CHAT,
    EMAIL,
    FACEBOOK,
    SMS,
    TWITTER,
    TELEGRAM;

    /**
     * Returns the enum constant as a fixed String value, e.g. to be used
     * as preference key. In case the keys are ever changed, this method
     * may be used to bring backward compatibility.
     * @return
     */
    public String getFixedValue() {
        return name().toLowerCase();
    }
}
