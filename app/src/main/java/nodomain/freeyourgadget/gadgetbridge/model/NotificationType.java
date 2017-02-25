package nodomain.freeyourgadget.gadgetbridge.model;

import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleColor;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleIconID;

public enum NotificationType {

    UNKNOWN(PebbleIconID.NOTIFICATION_GENERIC, PebbleColor.Red),

    CONVERSATIONS(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.Inchworm),
    GENERIC_EMAIL(PebbleIconID.GENERIC_EMAIL, PebbleColor.JaegerGreen),
    GENERIC_NAVIGATION(PebbleIconID.LOCATION, PebbleColor.Orange),
    GENERIC_SMS(PebbleIconID.GENERIC_SMS, PebbleColor.VividViolet),
    FACEBOOK(PebbleIconID.NOTIFICATION_FACEBOOK, PebbleColor.Liberty),
    FACEBOOK_MESSENGER(PebbleIconID.NOTIFICATION_FACEBOOK_MESSENGER, PebbleColor.VeryLightBlue),
    RIOT(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.LavenderIndigo),
    SIGNAL(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.BlueMoon),
    TWITTER(PebbleIconID.NOTIFICATION_TWITTER, PebbleColor.BlueMoon),
    TELEGRAM(PebbleIconID.NOTIFICATION_TELEGRAM, PebbleColor.PictonBlue),
    WHATSAPP(PebbleIconID.NOTIFICATION_WHATSAPP, PebbleColor.MayGreen),
    GENERIC_ALARM_CLOCK(PebbleIconID.ALARM_CLOCK, PebbleColor.Red);

    public int icon;
    public byte color;

    NotificationType(int icon, byte color) {
        this.icon = icon;
        this.color = color;
    }

    /**
     * Returns the enum constant as a fixed String value, e.g. to be used
     * as preference key. In case the keys are ever changed, this method
     * may be used to bring backward compatibility.
     */
    public String getFixedValue() {
        return name().toLowerCase();
    }

    public String getGenericType() {
        switch (this) {
            case GENERIC_EMAIL:
            case GENERIC_NAVIGATION:
            case GENERIC_SMS:
            case GENERIC_ALARM_CLOCK:
                return getFixedValue();
            case FACEBOOK:
            case TWITTER:
                return "generic_social";
            case CONVERSATIONS:
            case FACEBOOK_MESSENGER:
            case RIOT:
            case SIGNAL:
            case TELEGRAM:
            case WHATSAPP:
                return "generic_chat";
            case UNKNOWN:
            default:
                return "generic";
        }
    }
}
