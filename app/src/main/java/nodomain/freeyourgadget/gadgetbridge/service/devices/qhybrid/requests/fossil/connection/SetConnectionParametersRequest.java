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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.connection;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.fossil.FossilRequest;

public class SetConnectionParametersRequest extends FossilRequest {
    private boolean finished = false;

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public UUID getRequestUUID() {
        return UUID.fromString("3dda0002-957f-7d4a-34a6-74696673696d");
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        super.handleResponse(characteristic);
        this.finished = true;
    }

    @Override
    public byte[] getStartSequence() {
        return new byte[]{0x02, 0x09, 0x0C, 0x00, 0x0C, 0x00, 0x2D, 0x00, 0x58, 0x02};
    }

    @Override
    public boolean isBasicRequest() {
        return false;
    }
}
