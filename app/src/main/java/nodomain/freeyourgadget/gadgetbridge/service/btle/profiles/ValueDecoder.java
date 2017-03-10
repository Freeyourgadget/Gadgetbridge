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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles;

import android.bluetooth.BluetoothGattCharacteristic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.service.btle.GattCharacteristic;

public class ValueDecoder {
    private static final Logger LOG = LoggerFactory.getLogger(ValueDecoder.class);

    public static int decodePercent(BluetoothGattCharacteristic characteristic) {
        int percent = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        if (percent > 100 || percent < 0) {
            LOG.warn("Unexpected percent value: " + percent + ": " + GattCharacteristic.toString(characteristic));
            percent = Math.max(100, Math.min(0, percent));
        }
        return percent;
    }
}
