package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

/**
 * An adapter for notifications on AsteroidOS
 */
public class AsteroidOSNotification {
    private String packageName = null;
    private Integer id = null;
    private String applicationName = null;
    private String body = null;
    private String summary = null;
    private String icon = null;
    private Boolean remove = false;

    /**
     * The vibration strength of a notification
     */
    public enum VibrationStrength {
        STRONG,
        NORMAL,
        RINGTONE,
        NONE;

        @NonNull
        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
    private VibrationStrength vibrationStrength = VibrationStrength.NORMAL;

    /**
     * Creates a notification
     * @param spec The NotificationSpec to create the notification from
     */
    public AsteroidOSNotification(NotificationSpec spec) {
        this.body = spec.body;
        this.applicationName = spec.sourceName;
        this.summary = spec.subject;
        this.id = spec.getId();
        this.packageName = spec.sourceAppId;
    }

    /**
     * Creates a call notification
     * @param callSpec The callSpec given by the device support
     */
    public AsteroidOSNotification(CallSpec callSpec) {
        switch (callSpec.command) {
            case CallSpec.CALL_INCOMING:
                this.applicationName = GBApplication.getContext().getString(R.string.pref_screen_notification_profile_incoming_call);
                this.summary = callSpec.name;
                this.body = callSpec.number;
                this.vibrationStrength = VibrationStrength.RINGTONE;
                this.id = (callSpec.name + callSpec.number).hashCode();
                break;
            default:
                this.id = (callSpec.name + callSpec.number).hashCode();
                this.remove = true;
        }
    }

    /**
     * Creates a "remove" notification
     * @param id Notification ID to remove
     */
    public AsteroidOSNotification(int id) {
        this.id = id;
        this.remove = true;
    }

    @Override
    /**
     * Converts the notification to a string to be sent to the device
     */
    public String toString() {
        if (remove) {
            return "<remove><id>" + this.id + "</id></remove>";
        }
        String retString = new String();
        retString += "<insert>";
        if (id != null)
            retString += "<id>" + id + "</id>";
        retString += "<vb>" + vibrationStrength.toString() + "</vb>";
        if (packageName != null)
            retString += "<pn>" + packageName + "</pn>";
        if (applicationName != null)
            retString += "<an>" + applicationName + "</an>";
        if (icon != null)
            retString += "<ai>" + icon + "</ai>";
        if (summary != null)
            retString += "<su>" + summary + "</su>";
        if (body != null)
            retString += "<bo>" + body + "</bo>";
        retString += "</insert>";
        return retString;
    }
}
