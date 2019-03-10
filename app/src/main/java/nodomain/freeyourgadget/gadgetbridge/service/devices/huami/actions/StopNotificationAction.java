/*  Copyright (C) 2017-2019 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami.actions;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import nodomain.freeyourgadget.gadgetbridge.devices.huami.HuamiService;
import nodomain.freeyourgadget.gadgetbridge.service.btle.actions.AbortTransactionAction;

public abstract class StopNotificationAction extends AbortTransactionAction {

    private final BluetoothGattCharacteristic alertLevelCharacteristic;

    public StopNotificationAction(BluetoothGattCharacteristic alertLevelCharacteristic) {
        this.alertLevelCharacteristic = alertLevelCharacteristic;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        if (!super.run(gatt)) {
            // send a signal to stop the vibration
            alertLevelCharacteristic.setValue(new byte[]{HuamiService.ALERT_LEVEL_NONE});
            gatt.writeCharacteristic(alertLevelCharacteristic);
            return false;
        }
        return true;
    }
};

