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

    public void configure(TransactionBuilder builder, AlertNotificationControl control) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_NOTIFICATION_CONTROL_POINT);
        if (characteristic != null) {
            builder.write(characteristic, control.getControlMessage());
        }
    }

    public void updateAlertLevel(TransactionBuilder builder, AlertLevel level) {
        BluetoothGattCharacteristic characteristic = getCharacteristic(GattCharacteristic.UUID_CHARACTERISTIC_ALERT_LEVEL);
        if (characteristic != null) {
            builder.write(characteristic, new byte[] {BLETypeConversions.fromUint8(level.getId())});
        }
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

            boolean hasAlerted = false;
            for (int i = 0; i < numChunks; i++) {
                int offset = i * MAX_MSG_LENGTH;
                int restLength = message.length() - offset;
                message = message.substring(offset, offset + Math.min(MAX_MSG_LENGTH, restLength));
                if (hasAlerted && message.length() == 0) {
                    // no need to do it again when there is no text content
                    break;
                }
                writeAlertMessage(builder, characteristic, alert, message, i);
                hasAlerted = true;
            }
            if (!hasAlerted) {
                writeAlertMessage(builder, characteristic, alert, "", 1);
            }
        } else {
            LOG.warn("NEW_ALERT characteristic not available");
        }
    }

    protected void writeAlertMessage(TransactionBuilder builder, BluetoothGattCharacteristic characteristic, NewAlert alert, String message, int chunk) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream(100);
            stream.write(BLETypeConversions.fromUint8(alert.getCategory().getId()));
            stream.write(BLETypeConversions.fromUint8(alert.getNumAlerts()));

            if (message.length() > 0) {
                stream.write(BLETypeConversions.toUtf8s(message));
            } else {
                // some write a null byte instead of leaving out this optional value
//                stream.write(new byte[] {0});
            }
            builder.write(characteristic, stream.toByteArray());
        } catch (IOException ex) {
            // ain't gonna happen
            LOG.error("Error writing alert message to ByteArrayOutputStream");
        }
    }
}
