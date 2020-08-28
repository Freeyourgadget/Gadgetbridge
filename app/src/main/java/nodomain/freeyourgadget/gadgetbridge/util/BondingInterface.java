/*  Copyright (C) 2020 Taavi Eomäe

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
package nodomain.freeyourgadget.gadgetbridge.util;


import android.bluetooth.BluetoothDevice;
import android.content.Context;

public interface BondingInterface {
    /**
     * Called when pairing is complete
     **/
    void onBondingComplete(boolean success);

    /**
     * Should return the device that is currently being paired
     **/
    BluetoothDevice getCurrentTarget();

    /**
     * This forces bonding activities to encapsulate the removal
     * of all broadcast receivers on demand
     **/
    void unregisterBroadcastReceivers();

    /**
     * This forces bonding activities to handle the addition
     * of all broadcast receivers in the same place
     **/
    void removeBroadcastReceivers();

    /**
     * Just returns the Context
     */
    Context getContext();
}
