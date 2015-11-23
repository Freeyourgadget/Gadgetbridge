package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public interface NotificationStrategy {
    void sendDefaultNotification(TransactionBuilder builder, BtLEAction extraAction);

    /**
     * Adds a custom notification to the given transaction builder
     *
     * @param vibrationProfile specifies how and how often the Band shall vibrate.
     * @param flashTimes
     * @param flashColour
     * @param originalColour
     * @param flashDuration
     * @param extraAction      an extra action to be executed after every vibration and flash sequence. Allows to abort the repetition, for example.
     * @param builder
     */
    void sendCustomNotification(VibrationProfile vibrationProfile, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder);
}
