/*  Copyright (C) 2016-2018 Andreas Shimokawa, Carsten Pfeiffer

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

public abstract class ConditionalWriteAction extends WriteAction {
    public ConditionalWriteAction(BluetoothGattCharacteristic characteristic) {
        super(characteristic, null);
    }

    @Override
    protected boolean writeValue(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        byte[] conditionalValue = checkCondition();
        if (conditionalValue != null) {
            return super.writeValue(gatt, characteristic, conditionalValue);
        }
        return true;
    }

    /**
     * Checks the condition whether the write shall happen or not.
     * Returns the actual value to be written or null in case nothing shall be written.
     * <p/>
     * Note that returning null will not cause run() to return false, in other words,
     * the rest of the queue will still be executed.
     *
     * @return the value to be written or null to not write anything
     */
    protected abstract byte[] checkCondition();
}
