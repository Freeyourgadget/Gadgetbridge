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
