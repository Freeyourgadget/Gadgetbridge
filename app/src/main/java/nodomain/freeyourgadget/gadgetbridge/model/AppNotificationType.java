/*  Copyright (C) 2016-2017 Andreas Shimokawa, Daniele Gobbetti, Kevin Richter

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

import java.util.HashMap;

public class AppNotificationType extends HashMap<String, NotificationType> {

    private static AppNotificationType _instance;

    public static AppNotificationType getInstance() {
        if (_instance == null) {
            return (_instance = new AppNotificationType());
        }

        return _instance;
    }

    private AppNotificationType() {
        // Generic Email
        put("com.fsck.k9", NotificationType.GENERIC_EMAIL);
        put("com.fsck.k9.material", NotificationType.GENERIC_EMAIL);
        put("com.imaeses.squeaky", NotificationType.GENERIC_EMAIL);
        put("com.android.email", NotificationType.GENERIC_EMAIL);

        // Generic SMS
        put("com.moez.QKSMS", NotificationType.GENERIC_SMS);
        put("com.android.mms", NotificationType.GENERIC_SMS);
        put("com.android.messaging", NotificationType.GENERIC_SMS);
        put("com.sonyericsson.conversations", NotificationType.GENERIC_SMS);
        put("org.smssecure.smssecure", NotificationType.GENERIC_SMS);

        // Generic Calendar
        put("com.android.calendar", NotificationType.GENERIC_CALENDAR);

        // Google
        put("com.google.android.gm", NotificationType.GMAIL);
        put("com.google.android.apps.inbox", NotificationType.GOOGLE_INBOX);
        put("com.google.android.calendar", NotificationType.GENERIC_CALENDAR);
        put("com.google.android.apps.messaging", NotificationType.GOOGLE_MESSENGER);

        // Conversations
        put("eu.siacs.conversations", NotificationType.CONVERSATIONS);

        // Riot
        put("im.vector.alpha", NotificationType.RIOT);

        // Signal
        put("org.thoughtcrime.securesms", NotificationType.SIGNAL);

        // Telegram
        put("org.telegram.messenger", NotificationType.TELEGRAM);

        // Twitter
        put("org.mariotaku.twidere", NotificationType.TWITTER);
        put("com.twitter.android", NotificationType.TWITTER);
        put("org.andstatus.app", NotificationType.TWITTER);
        put("org.mustard.android", NotificationType.TWITTER);

        // Facebook
        put("me.zeeroooo.materialfb", NotificationType.FACEBOOK);
        put("it.rignanese.leo.slimfacebook", NotificationType.FACEBOOK);
        put("me.jakelane.wrapperforfacebook", NotificationType.FACEBOOK);
        put("com.facebook.katana", NotificationType.FACEBOOK);
        put("org.indywidualni.fblite", NotificationType.FACEBOOK);

        // Facebook Messenger
        put("com.facebook.orca", NotificationType.FACEBOOK_MESSENGER);

        // WhatsApp
        put("com.whatsapp", NotificationType.WHATSAPP);
    }

}
