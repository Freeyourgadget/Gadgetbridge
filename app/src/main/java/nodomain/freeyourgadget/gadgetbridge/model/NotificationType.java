/*  Copyright (C) 2015-2017 Andreas Shimokawa, AnthonyDiGirolamo, Carsten
    Pfeiffer, Frank Slezak, Julien Pivotto, Kaz Wolfe, Kevin Richter

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
    UNKNOWN(PebbleIconID.NOTIFICATION_GENERIC, PebbleColor.DarkCandyAppleRed),

    AMAZON(PebbleIconID.NOTIFICATION_AMAZON, PebbleColor.ChromeYellow),
    BBM(PebbleIconID.NOTIFICATION_BLACKBERRY_MESSENGER, PebbleColor.DarkGray),
    CONVERSATIONS(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.Inchworm),
    FACEBOOK(PebbleIconID.NOTIFICATION_FACEBOOK, PebbleColor.CobaltBlue),
    FACEBOOK_MESSENGER(PebbleIconID.NOTIFICATION_FACEBOOK_MESSENGER, PebbleColor.BlueMoon),
    GENERIC_ALARM_CLOCK(PebbleIconID.ALARM_CLOCK, PebbleColor.Red),
    GENERIC_CALENDAR(PebbleIconID.TIMELINE_CALENDAR, PebbleColor.BlueMoon),
    GENERIC_EMAIL(PebbleIconID.GENERIC_EMAIL, PebbleColor.Orange),
    GENERIC_NAVIGATION(PebbleIconID.LOCATION, PebbleColor.Orange),
    GENERIC_PHONE(PebbleIconID.DURING_PHONE_CALL, PebbleColor.JaegerGreen),
    GENERIC_SMS(PebbleIconID.GENERIC_SMS, PebbleColor.VividViolet),
    GMAIL(PebbleIconID.NOTIFICATION_GMAIL, PebbleColor.Red),
    GOOGLE_HANGOUTS(PebbleIconID.NOTIFICATION_GOOGLE_HANGOUTS, PebbleColor.JaegerGreen),
    GOOGLE_INBOX(PebbleIconID.NOTIFICATION_GOOGLE_INBOX, PebbleColor.BlueMoon),
    GOOGLE_MAPS(PebbleIconID.NOTIFICATION_GOOGLE_MAPS, PebbleColor.BlueMoon),
    GOOGLE_MESSENGER(PebbleIconID.NOTIFICATION_GOOGLE_MESSENGER, PebbleColor.VividCerulean),
    GOOGLE_PHOTOS(PebbleIconID.NOTIFICATION_GOOGLE_PHOTOS, PebbleColor.BlueMoon),
    HIPCHAT(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.CobaltBlue),
    INSTAGRAM(PebbleIconID.NOTIFICATION_INSTAGRAM, PebbleColor.CobaltBlue),
    KAKAO_TALK(PebbleIconID.NOTIFICATION_KAKAOTALK, PebbleColor.Yellow),
    KIK(PebbleIconID.NOTIFICATION_KIK, PebbleColor.IslamicGreen),
    LIGHTHOUSE(PebbleIconID.NOTIFICATION_LIGHTHOUSE, PebbleColor.PictonBlue), // ??? - No idea what this is, but it works.
    LINE(PebbleIconID.NOTIFICATION_LINE, PebbleColor.IslamicGreen),
    LINKEDIN(PebbleIconID.NOTIFICATION_LINKEDIN, PebbleColor.CobaltBlue),
    MAILBOX(PebbleIconID.NOTIFICATION_MAILBOX, PebbleColor.VividCerulean),
    OUTLOOK(PebbleIconID.NOTIFICATION_OUTLOOK, PebbleColor.BlueMoon),
    BUSINESS_CALENDAR(PebbleIconID.TIMELINE_CALENDAR, PebbleColor.BlueMoon),
    RIOT(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.LavenderIndigo),
    SIGNAL(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.BlueMoon),
    SKYPE(PebbleIconID.NOTIFICATION_SKYPE, PebbleColor.VividCerulean),
    SLACK(PebbleIconID.NOTIFICATION_SLACK, PebbleColor.Folly),
    SNAPCHAT(PebbleIconID.NOTIFICATION_SNAPCHAT, PebbleColor.Icterine),
    TELEGRAM(PebbleIconID.NOTIFICATION_TELEGRAM, PebbleColor.VividCerulean),
    THREEMA(PebbleIconID.NOTIFICATION_HIPCHAT, PebbleColor.JaegerGreen),
    TRANSIT(PebbleIconID.LOCATION, PebbleColor.JaegerGreen),
    TWITTER(PebbleIconID.NOTIFICATION_TWITTER, PebbleColor.BlueMoon),
    VIBER(PebbleIconID.NOTIFICATION_VIBER, PebbleColor.VividViolet),
    WECHAT(PebbleIconID.NOTIFICATION_WECHAT, PebbleColor.KellyGreen),
    WHATSAPP(PebbleIconID.NOTIFICATION_WHATSAPP, PebbleColor.IslamicGreen),
    YAHOO_MAIL(PebbleIconID.NOTIFICATION_YAHOO_MAIL, PebbleColor.Indigo);

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
            case SNAPCHAT:
            case INSTAGRAM:
            case LINKEDIN:
                return "generic_social";
            case CONVERSATIONS:
            case FACEBOOK_MESSENGER:
            case RIOT:
            case SIGNAL:
            case TELEGRAM:
            case THREEMA:
            case WHATSAPP:
            case GOOGLE_MESSENGER:
            case GOOGLE_HANGOUTS:
            case HIPCHAT:
            case SKYPE:
            case WECHAT:
            case KIK:
            case KAKAO_TALK:
            case SLACK:
            case LINE:
            case VIBER:
                return "generic_chat";
            case GMAIL:
            case GOOGLE_INBOX:
            case MAILBOX:
            case OUTLOOK:
            case YAHOO_MAIL:
                return "generic_email";
            case UNKNOWN:
            default:
                return "generic";
        }
    }
}
