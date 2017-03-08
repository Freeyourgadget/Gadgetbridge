package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertNotificationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.NewAlert;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.OverflowStrategy;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;

public class V2NotificationStrategy implements NotificationStrategy {
    private final AbstractBTLEDeviceSupport support;

    public V2NotificationStrategy(AbstractBTLEDeviceSupport support) {
        this.support = support;
    }

    protected AbstractBTLEDeviceSupport getSupport() {
        return support;
    }

    @Override
    public void sendDefaultNotification(TransactionBuilder builder, SimpleNotification simpleNotification, BtLEAction extraAction) {
        VibrationProfile profile = VibrationProfile.getProfile(VibrationProfile.ID_MEDIUM, (short) 3);
        sendCustomNotification(profile, simpleNotification, extraAction, builder);
    }

    protected void sendCustomNotification(VibrationProfile vibrationProfile, SimpleNotification simpleNotification, BtLEAction extraAction, TransactionBuilder builder) {
        //use the new alert characteristic
        BluetoothGattCharacteristic alert = support.getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL);
        for (short i = 0; i < vibrationProfile.getRepeat(); i++) {
            int[] onOffSequence = vibrationProfile.getOnOffSequence();
            for (int j = 0; j < onOffSequence.length; j++) {
                int on = onOffSequence[j];
                on = Math.min(500, on); // longer than 500ms is not possible
                builder.write(alert, new byte[]{GattCharacteristic.MILD_ALERT}); //MILD_ALERT lights up GREEN leds, HIGH_ALERT lights up RED leds
//                builder.wait(on);
//                builder.write(alert, new byte[]{GattCharacteristic.HIGH_ALERT});
                builder.wait(on);
                builder.write(alert, new byte[]{GattCharacteristic.NO_ALERT});

                if (++j < onOffSequence.length) {
                    int off = Math.max(onOffSequence[j], 25); // wait at least 25ms
                    builder.wait(off);
                }

                if (extraAction != null) {
                    builder.add(extraAction);
                }
            }
        }
//        sendAlert(simpleNotification, builder);
    }

    protected void sendAlert(SimpleNotification simpleNotification, TransactionBuilder builder) {
        AlertNotificationProfile<?> profile = new AlertNotificationProfile<>(getSupport());
        NewAlert alert = new NewAlert(simpleNotification.getAlertCategory(), 1, simpleNotification.getMessage());
        profile.newAlert(builder, alert, OverflowStrategy.MAKE_MULTIPLE);
    }

    @Override
    public void sendCustomNotification(VibrationProfile vibrationProfile, SimpleNotification simpleNotification, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        // all other parameters are unfortunately not supported anymore ;-(
        sendCustomNotification(vibrationProfile, simpleNotification, extraAction, builder);
    }
}
