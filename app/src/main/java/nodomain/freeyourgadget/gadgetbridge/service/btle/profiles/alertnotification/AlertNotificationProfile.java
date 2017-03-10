/*  Copyright (C) 2016-2017 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractBTLEDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.AbstractBleProfile;
import nodomain.freeyourgadget.gadgetbridge.util.StringUtils;

public class AlertNotificationProfile<T extends AbstractBTLEDeviceSupport> extends AbstractBleProfile<T> {
    private static final Logger LOG = LoggerFactory.getLogger(AlertNotificationProfile.class);
    private static final int MAX_MSG_LENGTH = 18;

    public AlertNotificationProfile(T support) {
        super(support);
    }

    public void newAlert(TransactionBuilder builder, NewAlert alert, OverflowStrategy strategy) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_NEW_ALERT);
        if (characteristic != null) {
            String message = alert.getMessage();
            if (message.length() > MAX_MSG_LENGTH && strategy == OverflowStrategy.TRUNCATE) {
                message = StringUtils.truncate(message, MAX_MSG_LENGTH);
            }

            int numChunks = message.length() / MAX_MSG_LENGTH;
            if (message.length() % MAX_MSG_LENGTH > 0) {
                numChunks++;
            }

            for (int i = 0; i < numChunks; i++) {
                int offset = i * MAX_MSG_LENGTH;
                int restLength = message.length() - offset;
                message = message.substring(offset, offset + Math.min(MAX_MSG_LENGTH, restLength));
                if (message.length() == 0) {
                    break;
                }
                writeAlertMessage(builder, characteristic, alert, message, i);
            }
        } else {
            LOG.warn("NEW_ALERT characteristic not available");
        }
    }

    protected void writeAlertMessage(TransactionBuilder builder, BluetoothGattCharacteristic characteristic, NewAlert alert, String message, int chunk) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream(100);
            stream.write(alert.getCategory().getId());
            stream.write(alert.getNumAlerts());
            stream.write(BLETypeConversions.toUtf8s(message));

            builder.write(characteristic, stream.toByteArray());
        } catch (IOException ex) {
            // aint gonna happen
            LOG.error("Error writing alert message to ByteArrayOutputStream");
        }
    }
}
