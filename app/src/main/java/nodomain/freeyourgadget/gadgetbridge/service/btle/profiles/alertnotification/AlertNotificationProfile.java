/*  Copyright (C) 2016-2019 Andreas Shimokawa, Carsten Pfeiffer

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
    private int maxLength = 18; // Mi2-ism?

    public AlertNotificationProfile(T support) {
        super(support);
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
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
            String message = StringUtils.ensureNotNull(alert.getMessage());
            if (message.length() > maxLength && strategy == OverflowStrategy.TRUNCATE) {
                message = StringUtils.truncate(message, maxLength);
            }

            int numChunks = message.length() / maxLength;
            if (message.length() % maxLength > 0) {
                numChunks++;
            }

            try {
                boolean hasAlerted = false;
                for (int i = 0; i < numChunks; i++) {
                    int offset = i * maxLength;
                    int restLength = message.length() - offset;
                    message = message.substring(offset, offset + Math.min(maxLength, restLength));
                    if (hasAlerted && message.length() == 0) {
                        // no need to do it again when there is no text content
                        break;
                    }
                    builder.write(characteristic, getAlertMessage(alert, message, 1));
                    hasAlerted = true;
                }
                if (!hasAlerted) {
                    builder.write(characteristic, getAlertMessage(alert, "", 1));
                }
            } catch (IOException ex) {
                // ain't gonna happen
                LOG.error("Error writing alert message to ByteArrayOutputStream");
            }
        } else {
            LOG.warn("NEW_ALERT characteristic not available");
        }
    }

    public void newAlert(TransactionBuilder builder, NewAlert alert) {
        newAlert(builder, alert, OverflowStrategy.TRUNCATE);
    }

    protected byte[] getAlertMessage(NewAlert alert, String message, int chunk) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(100);
        stream.write(BLETypeConversions.fromUint8(alert.getCategory().getId()));
        stream.write(BLETypeConversions.fromUint8(alert.getNumAlerts()));
        if (alert.getCategory() == AlertCategory.CustomHuami) {
            stream.write(BLETypeConversions.fromUint8(alert.getCustomIcon()));
        }

        if (message.length() > 0) {
            stream.write(BLETypeConversions.toUtf8s(message));
        } else {
            // some write a null byte instead of leaving out this optional value
//                stream.write(new byte[] {0});
        }
        return stream.toByteArray();
    }
}
