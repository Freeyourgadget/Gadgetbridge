/*  Copyright (C) 2015-2024 Alicia Hormann, Carsten Pfeiffer, Daniele Gobbetti

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothStatusCodes;
import android.os.Build;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

import static nodomain.freeyourgadget.gadgetbridge.service.btle.GattDescriptor.UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION;

import androidx.annotation.RequiresPermission;

/**
 * Enables or disables notifications for a given GATT characteristic.
 * The result will be made available asynchronously through the
 * {@link BluetoothGattCallback}.
 */
public class NotifyAction extends BtLEAction {

    private static final Logger LOG = LoggerFactory.getLogger(NotifyAction.class);
    protected final boolean enableFlag;
    private boolean hasWrittenDescriptor = false;

    public NotifyAction(BluetoothGattCharacteristic characteristic, boolean enable) {
        super(characteristic);
        enableFlag = enable;
    }

    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    private boolean writeDescriptor(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final byte[] value) {
        if (gatt == null) {
            LOG.error("gatt == null");
            return false;
        }

        if (descriptor == null) {
            LOG.error("descriptor == null");
            return false;
        }

        final String charUuid = descriptor.getCharacteristic().getUuid().toString();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // use API introduced in SDK level 33 to catch exceptions and more specific errors
            try {
                final int result = gatt.writeDescriptor(descriptor, value);

                if (result != BluetoothStatusCodes.SUCCESS) {
                    LOG.error("Writing characteristic {} descriptor failed: {}", charUuid, result);
                    return false;
                }
            } catch (final SecurityException ex) {
                LOG.error("SecurityException while writing to characteristic {} descriptor: {}", charUuid, ex.getMessage(), ex);
                return false;
            }
        } else {
            if (!descriptor.setValue(value)) {
                LOG.error("Updating descriptor value on characteristic {} failed", charUuid);
                return false;
            }

            if (!gatt.writeDescriptor(descriptor)) {
                LOG.error("Writing descriptor on characteristic {} failed", charUuid);
                return false;
            }
        }

        LOG.debug("Successfully written characteristic {} descriptor", charUuid);
        return true;
    }

    @Override
    @RequiresPermission("android.permission.BLUETOOTH_CONNECT")
    public boolean run(BluetoothGatt gatt) {
        // register gatt's callback to receive notifications
        boolean result = gatt.setCharacteristicNotification(getCharacteristic(), enableFlag);

        if (result) {
            BluetoothGattDescriptor clientCharConfigDescriptor = getCharacteristic().getDescriptor(UUID_DESCRIPTOR_GATT_CLIENT_CHARACTERISTIC_CONFIGURATION);

            if (clientCharConfigDescriptor != null) {
                int properties = getCharacteristic().getProperties();

                if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    LOG.debug("use NOTIFICATION for Characteristic " + getCharacteristic().getUuid());
                    final byte[] value = enableFlag ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                    result = writeDescriptor(gatt, clientCharConfigDescriptor, value);
                    hasWrittenDescriptor = true;
                } else if ((properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
                    LOG.debug("use INDICATION for Characteristic " + getCharacteristic().getUuid());
                    final byte[] value = enableFlag ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE;
                    result =  writeDescriptor(gatt, clientCharConfigDescriptor, value);
                    hasWrittenDescriptor = true;
                } else {
                    LOG.debug("use neither NOTIFICATION nor INDICATION for Characteristic " + getCharacteristic().getUuid());
                    hasWrittenDescriptor = false;
                }
            } else {
                LOG.warn("Descriptor CLIENT_CHARACTERISTIC_CONFIGURATION for characteristic " + getCharacteristic().getUuid() + " is null");
                hasWrittenDescriptor = false;
            }
        } else {
            LOG.error("Unable to enable notifications for " + getCharacteristic().getUuid());
            hasWrittenDescriptor = false;
        }

        return result;
    }

    @Override
    public boolean expectsResult() {
        return hasWrittenDescriptor;
    }
}
