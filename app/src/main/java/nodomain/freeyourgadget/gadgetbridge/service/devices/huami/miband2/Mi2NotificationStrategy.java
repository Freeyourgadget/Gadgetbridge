/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Martin Piatka

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.miband2;

import android.bluetooth.BluetoothGattCharacteristic;
import android.support.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.V2NotificationStrategy;

public class Mi2NotificationStrategy extends V2NotificationStrategy<MiBand2Support> {

    private final BluetoothGattCharacteristic alertLevelCharacteristic;

    public Mi2NotificationStrategy(MiBand2Support support) {
        super(support);
        alertLevelCharacteristic = support.getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL);
    }

    @Override
    protected void sendCustomNotification(VibrationProfile vibrationProfile, SimpleNotification simpleNotification, BtLEAction extraAction, TransactionBuilder builder) {
        startNotify(builder, vibrationProfile.getAlertLevel(), simpleNotification);
        BluetoothGattCharacteristic alert = getSupport().getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL);
        byte repeat = (byte) (vibrationProfile.getRepeat() * (vibrationProfile.getOnOffSequence().length / 2));
        int waitDuration = 0;
        if (repeat > 0) {
            short vibration = (short) vibrationProfile.getOnOffSequence()[0];
            short pause = (short) vibrationProfile.getOnOffSequence()[1];
            waitDuration = (vibration + pause) * repeat;
            builder.write(alert, new byte[]{-1, (byte) (vibration & 255), (byte) (vibration >> 8 & 255), (byte) (pause & 255), (byte) (pause >> 8 & 255), repeat});
        }

        waitDuration = Math.max(waitDuration, 4000);
        builder.wait(waitDuration);

        if (extraAction != null) {
            builder.add(extraAction);
        }
    }

    protected void startNotify(TransactionBuilder builder, int alertLevel, @Nullable SimpleNotification simpleNotification) {
        builder.write(alertLevelCharacteristic, new byte[] {(byte) alertLevel});

    }

    protected void stopNotify(TransactionBuilder builder) {
        builder.write(alertLevelCharacteristic, new byte[]{GattCharacteristic.NO_ALERT});
    }

    @Override
    public void sendCustomNotification(VibrationProfile vibrationProfile, @Nullable SimpleNotification simpleNotification, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        // all other parameters are unfortunately not supported anymore ;-(
        sendCustomNotification(vibrationProfile, simpleNotification, extraAction, builder);
    }
}
