/*  Copyright (C) 2019-2020 Daniel Dakhno

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
import android.os.Build;

import androidx.annotation.RequiresApi;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BtLEAction;

public class RequestMtuAction extends BtLEAction {
    private int mtu;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public RequestMtuAction(int mtu) {
        super(null);
        this.mtu = mtu;
    }


    @Override
    public boolean expectsResult() {
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean run(BluetoothGatt gatt) {
        return gatt.requestMtu(this.mtu);
    }
}
