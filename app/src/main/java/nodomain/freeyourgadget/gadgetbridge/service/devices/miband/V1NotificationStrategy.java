package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandService;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;

public class V1NotificationStrategy implements NotificationStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(V1NotificationStrategy.class);

    static final byte[] startVibrate = new byte[]{MiBandService.COMMAND_SEND_NOTIFICATION, 1};
    static final byte[] stopVibrate = new byte[]{MiBandService.COMMAND_STOP_MOTOR_VIBRATE};

    private final MiBandSupport support;

    public V1NotificationStrategy(MiBandSupport support) {
        this.support = support;
    }

    @Override
    public void sendDefaultNotification(TransactionBuilder builder, BtLEAction extraAction) {
        BluetoothGattCharacteristic characteristic = support.getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        builder.write(characteristic, getDefaultNotification());
        builder.add(extraAction);
    }

    private byte[] getDefaultNotification() {
        final int vibrateTimes = 1;
        final long vibrateDuration = 250l;
        final int flashTimes = 1;
        final int flashColour = 0xFFFFFFFF;
        final int originalColour = 0xFFFFFFFF;
        final long flashDuration = 250l;

        return getNotification(vibrateDuration, vibrateTimes, flashTimes, flashColour, originalColour, flashDuration);
    }

    private byte[] getNotification(long vibrateDuration, int vibrateTimes, int flashTimes, int flashColour, int originalColour, long flashDuration) {
        byte[] vibrate = startVibrate;
        byte r = 6;
        byte g = 0;
        byte b = 6;
        boolean display = true;
        //      byte[] flashColor = new byte[]{ 14, r, g, b, display ? (byte) 1 : (byte) 0 };
        return vibrate;
    }

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
    public void sendCustomNotification(VibrationProfile vibrationProfile, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        BluetoothGattCharacteristic controlPoint = support.getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
        for (short i = 0; i < vibrationProfile.getRepeat(); i++) {
            int[] onOffSequence = vibrationProfile.getOnOffSequence();
            for (int j = 0; j < onOffSequence.length; j++) {
                int on = onOffSequence[j];
                on = Math.min(500, on); // longer than 500ms is not possible
                builder.write(controlPoint, startVibrate);
                builder.wait(on);
                builder.write(controlPoint, stopVibrate);

                if (++j < onOffSequence.length) {
                    int off = Math.max(onOffSequence[j], 25); // wait at least 25ms
                    builder.wait(off);
                }

                if (extraAction != null) {
                    builder.add(extraAction);
                }
            }
        }
    }

//    private void sendCustomNotification(int vibrateDuration, int vibrateTimes, int pause, int flashTimes, int flashColour, int originalColour, long flashDuration, TransactionBuilder builder) {
//        BluetoothGattCharacteristic controlPoint = getCharacteristic(MiBandService.UUID_CHARACTERISTIC_CONTROL_POINT);
//        int vDuration = Math.min(500, vibrateDuration); // longer than 500ms is not possible
//        for (int i = 0; i < vibrateTimes; i++) {
//            builder.write(controlPoint, startVibrate);
//            builder.wait(vDuration);
//            builder.write(controlPoint, stopVibrate);
//            if (pause > 0) {
//                builder.wait(pause);
//            }
//        }
//
//        LOG.info("Sending notification to MiBand: " + controlPoint);
//        builder.queue(getQueue());
//    }
}
