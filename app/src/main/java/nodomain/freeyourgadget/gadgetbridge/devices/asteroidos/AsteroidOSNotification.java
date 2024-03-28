/*  Copyright (C) 2022-2024 Noodlez

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import androidx.annotation.NonNull;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;

/**
 * An adapter for notifications on AsteroidOS
 */
public class AsteroidOSNotification {
    private String packageName = null;
    private Integer id;
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
        this.icon = this.gbNotificationTypeToIcon(spec.type);
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
                this.icon = "ios-call-outline";
                break;
            case CallSpec.CALL_OUTGOING:
                break;
            case CallSpec.CALL_REJECT:
            case CallSpec.CALL_ACCEPT:
            case CallSpec.CALL_END:
            case CallSpec.CALL_START:
            case CallSpec.CALL_UNDEFINED:
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

    /**
     * Converts the notification to a string to be sent to the device
     */
    @NonNull
    @Override
    public String toString() {
        if (remove) {
            return "<removed><id>" + this.id + "</id></removed>";
        }
        String retString = "";
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

    public String gbNotificationTypeToIcon(NotificationType type) {
        switch (type) {
            // Logos
            case FACEBOOK:
                return "logo-facebook";
            case INSTAGRAM:
                return "logo-instagram";
            case LINKEDIN:
                return "logo-linkedin";
            case SIGNAL:
                return "logo-signal";
            case SKYPE:
                return "logo-skype";
            case SNAPCHAT:
                return "logo-snapchat";
            case TWITTER:
                return "logo-twitter";
            case WHATSAPP:
                return "logo-whatsapp";
            case YAHOO_MAIL:
                return "logo-yahoo";
            // Generic
            default:
                switch (type.getGenericType()) {
                    case "generic_email":
                        return "ios-mail-outline";
                    case "generic_navigation":
                        return "ios-navigate-outline";
                    case "generic_sms":
                        return "ios-chatboxes-outline";
                    case "generic_alarm_clock":
                        return "ios-alarm-outline";
                    case "generic_social":
                        return "ios-people-outline";
                    case "generic_chat":
                        return "ios-chatbubbles-outline";
                    default:
                        return "ios-notifications-outline";
                }
        }
    }
}
