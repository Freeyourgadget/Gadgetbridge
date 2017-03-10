/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband2;

import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.VibrationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertNotificationProfile;
import nodomain.freeyourgadget.gadgetbridge.service.devices.common.SimpleNotification;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.V2NotificationStrategy;

public class Mi2NotificationStrategy extends V2NotificationStrategy {

    public Mi2NotificationStrategy(AbstractBTLEDeviceSupport support) {
        super(support);
    }

    @Override
    protected void sendCustomNotification(VibrationProfile vibrationProfile, SimpleNotification simpleNotification, BtLEAction extraAction, TransactionBuilder builder) {
        //use the new alert characteristic
        BluetoothGattCharacteristic alert = getSupport().getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL);
        for (short i = 0; i < vibrationProfile.getRepeat(); i++) {
            int[] onOffSequence = vibrationProfile.getOnOffSequence();
            for (int j = 0; j < onOffSequence.length; j++) {
                int on = onOffSequence[j];
                on = Math.min(500, on); // longer than 500ms is not possible
                builder.write(alert, new byte[]{(byte) vibrationProfile.getAlertLevel()});
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

        sendAlert(simpleNotification, builder);
    }

    @Override
    public void sendCustomNotification(VibrationProfile vibrationProfile, SimpleNotification simpleNotification, int flashTimes, int flashColour, int originalColour, long flashDuration, BtLEAction extraAction, TransactionBuilder builder) {
        // all other parameters are unfortunately not supported anymore ;-(
        sendCustomNotification(vibrationProfile, simpleNotification, extraAction, builder);
    }
}
