package nodomain.freeyourgadget.gadgetbridge.model;

public enum NotificationType {

    UNKNOWN,

    CONVERSATIONS,
    GENERIC_EMAIL,
    GENERIC_NAVIGATION,
    GENERIC_SMS,
    FACEBOOK,
    FACEBOOK_MESSENGER,
    SIGNAL,
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

    public String getGenericType() {
        switch (this) {
            case GENERIC_EMAIL:
            case GENERIC_NAVIGATION:
            case GENERIC_SMS:
                return getFixedValue();
            case FACEBOOK:
            case TWITTER:
                return "generic_social";
            case CONVERSATIONS:
            case FACEBOOK_MESSENGER:
            case SIGNAL:
            case TELEGRAM:
                return "generic_chat";
            case UNKNOWN:
            default:
                return "generic";
        }
    }
}
