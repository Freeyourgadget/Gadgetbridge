package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import android.content.Context;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

public class AsteroidOSNotification {
    private String packageName = null;
    private Integer id = null;
    private String applicationName = null;
    private String body = null;
    private String summary = null;
    private String icon = null;
    private Boolean remove = false;

    public enum VibrationStrength {
        STRONG,
        NORMAL,
        RINGTONE,
        NONE;

        @NonNull
        @Override
        public String toString() {
            switch (this) {
                case STRONG:
                    return "strong";
                case NORMAL:
                    return "normal";
                case RINGTONE:
                    return "ringtone";
                case NONE:
                    return "none";
            }
            return "";
        }
    }
    private VibrationStrength vibrationStrength = VibrationStrength.NORMAL;
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
                break;
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
