/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.cmfwatchpro;

import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;

public enum CmfNotificationIcon {
    GENERIC_SMS(0),
    WHATSAPP(8),
    SNAPCHAT(9),
    WHATSAPP_BUSINESS(10),
    TRUECALLER(11), // blue phone
    TELEGRAM(12),
    FACEBOOK_MESSENGER(13),
    IMO(14),
    CALLAPP(15),
    FACEBOOK(17),
    INSTAGRAM(18),
    TIKTOK(19),
    LINE(20),
    DISCORD(21),
    GOOGLE_VOICE(22),
    GMAIL(27),
    OUTLOOK(29),
    UNKNOWN(255),
    ;

    private final byte code;

    CmfNotificationIcon(final int code) {
        this.code = (byte) code;
    }

    public byte getCode() {
        return code;
    }

    public static CmfNotificationIcon forNotification(final NotificationSpec notificationSpec) {
        if (notificationSpec.type == null) {
            return UNKNOWN;
        }

        try {
            // If there's a matching enum, just return it
            return CmfNotificationIcon.valueOf(notificationSpec.type.name());
        } catch (final IllegalArgumentException ignored) {
            // ignored
        }

        switch (notificationSpec.type.getGenericType()) {
            case "generic_chat":
                return GENERIC_SMS;
            case "generic_email":
                return GMAIL;
            case "generic_phone":
                return TRUECALLER;
        }

        return UNKNOWN;
    }
}
