/*  Copyright (C) 2020-2023 Petr Kadlec

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr.messages;

public class NotificationServiceSubscriptionMessage {
    public static final int INTENT_UNSUBSCRIBE = 0;
    public static final int INTENT_SUBSCRIBE = 1;
    private static final int FEATURE_FLAG_PHONE_NUMBER = 1;

    public final int intentIndicator;
    public final int featureFlags;

    public NotificationServiceSubscriptionMessage(int intentIndicator, int featureFlags) {
        this.intentIndicator = intentIndicator;
        this.featureFlags = featureFlags;
    }

    public boolean isSubscribe() {
        return intentIndicator == INTENT_SUBSCRIBE;
    }

    public boolean hasPhoneNumberSupport() {
        return (featureFlags & FEATURE_FLAG_PHONE_NUMBER) != 0;
    }

    public static NotificationServiceSubscriptionMessage parsePacket(byte[] packet) {
        final MessageReader reader = new MessageReader(packet, 4);

        final int intentIndicator = reader.readByte();
        final int featureFlags = packet.length > 7 ? reader.readByte() : 0;

        return new NotificationServiceSubscriptionMessage(intentIndicator, featureFlags);
    }
}
