/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer

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

/**
 * An action that will cause the queue to sleep for the specified time.
 * Note that this is usually a bad idea, since it will not be able to process messages
 * during that time. It is also likely to cause race conditions.
 */
public class WaitAction extends PlainAction {

    private final int mMillis;

    public WaitAction(int millis) {
        mMillis = millis;
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        try {
            Thread.sleep(mMillis);
            return true;
        } catch (InterruptedException e) {
            return false;
        }
    }
}
