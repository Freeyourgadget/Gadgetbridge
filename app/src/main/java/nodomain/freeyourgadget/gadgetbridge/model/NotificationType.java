/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Julien
    Pivotto, Kevin Richter

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.model;

import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleColor;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleIconID;

public enum NotificationType {

    // TODO: this this pebbleism needs to be moved somewhere else
    UNKNOWN(PebbleIconID.NOTIFICATION_GENERIC, PebbleColor.Red),

    CONVERSATIONS(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.Inchworm),
    GENERIC_EMAIL(PebbleIconID.GENERIC_EMAIL, PebbleColor.JaegerGreen),
    GENERIC_NAVIGATION(PebbleIconID.LOCATION, PebbleColor.Orange),
    GENERIC_SMS(PebbleIconID.GENERIC_SMS, PebbleColor.VividViolet),
    GENERIC_CALENDAR(PebbleIconID.TIMELINE_CALENDAR, PebbleColor.Blue),
    FACEBOOK(PebbleIconID.NOTIFICATION_FACEBOOK, PebbleColor.Liberty),
    FACEBOOK_MESSENGER(PebbleIconID.NOTIFICATION_FACEBOOK_MESSENGER, PebbleColor.VeryLightBlue),
    RIOT(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.LavenderIndigo),
    SIGNAL(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.BlueMoon),
    TWITTER(PebbleIconID.NOTIFICATION_TWITTER, PebbleColor.BlueMoon),
    TELEGRAM(PebbleIconID.NOTIFICATION_TELEGRAM, PebbleColor.PictonBlue),
    WHATSAPP(PebbleIconID.NOTIFICATION_WHATSAPP, PebbleColor.MayGreen),
    GENERIC_ALARM_CLOCK(PebbleIconID.ALARM_CLOCK, PebbleColor.Red);
    // Note: if you add any more constants, update all clients as well

    public final int icon;
    public final byte color;

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
