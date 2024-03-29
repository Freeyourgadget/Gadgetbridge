/*  Copyright (C) 2022-2024 Damien Gaignon

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr.actions;

import android.bluetooth.BluetoothSocket;
import android.content.Context;

import androidx.annotation.NonNull;

import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class SetDeviceStateAction extends PlainAction {
    private final GBDevice device;
    private final GBDevice.State deviceState;
    private final Context context;

    public SetDeviceStateAction(GBDevice device, GBDevice.State deviceState, Context context) {
        this.device = device;
        this.deviceState = deviceState;
        this.context = context;
    }

    @Override
    public boolean run(BluetoothSocket socket) {
        device.setState(deviceState);
        device.sendDeviceUpdateIntent(getContext(), GBDevice.DeviceUpdateSubject.DEVICE_STATE);
        return true;
    }

    public Context getContext() {
        return context;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + " to " + deviceState;
    }
}
