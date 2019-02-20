/*  Copyright (C) 2018-2019 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.service.btle.AbstractGattCallback;
import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEQueue;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCallback;
import nodomain.freeyourgadget.gadgetbridge.service.btle.GattListenerAction;

public abstract class AbstractGattListenerWriteAction extends WriteAction implements GattListenerAction {
    private final BtLEQueue queue;

    public AbstractGattListenerWriteAction(BtLEQueue queue, BluetoothGattCharacteristic characteristic, byte[] value) {
        super(characteristic, value);
        this.queue = queue;
        Objects.requireNonNull(queue, "queue must not be null");
    }

    @Override
    public GattCallback getGattCallback() {
        return new AbstractGattCallback() {
            @Override
            public boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                return AbstractGattListenerWriteAction.this.onCharacteristicChanged(gatt, characteristic);
            }
        };
    }

    protected abstract boolean onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);
}
