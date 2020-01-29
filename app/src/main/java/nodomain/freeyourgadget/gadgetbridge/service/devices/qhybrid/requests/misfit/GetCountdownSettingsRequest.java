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
package nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.misfit;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.service.devices.qhybrid.requests.Request;

public class GetCountdownSettingsRequest extends Request {
    @Override
    public byte[] getStartSequence() {
        return new byte[]{1, 19, 1};
    }

    @Override
    public void handleResponse(BluetoothGattCharacteristic characteristic) {
        byte[] value = characteristic.getValue();
        if (value.length != 14) {
            return;
        }
        ByteBuffer buffer = ByteBuffer.wrap(value);
        long startTime = j(buffer.getInt(3));
        long endTime = j(buffer.getInt(7));
        byte progress = buffer.get(13);
        short offset = buffer.getShort(11);

        log("progress: " + progress);

    }


    public static long j(final int n) {
        if (n < 0) {
            return 4294967296L + n;
        }
        return n;
    }
}
