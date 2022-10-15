/*  Copyright (C) 2015-2021 Andreas Shimokawa, Carsten Pfeiffer, Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

import android.bluetooth.BluetoothSocket;

import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;

/**
 * This allows performing one socket request at a time.
 * As they are asynchronous anyway, we encapsulate every socket request (only write)
 * inside a runnable action.
 * <p/>
 * These actions are then executed one after another, ensuring that every action's result
 * has been posted before invoking the next action.
 * <p/>
 * As there is only write action, this class will be needed for later usage.
 */
public abstract class BtBRAction {
    private final long creationTimestamp;

    public BtBRAction() {
        creationTimestamp = System.currentTimeMillis();
    }

    /**
     * Returns true if this action expects an (async) result which must
     * be waited for, before continuing with other actions.
     * <p/>
     */
    public abstract boolean expectsResult();

    /**
     * Executes this action, e.g. reads or write a Socket characteristic.
     *
     * @param socket the characteristic to manipulate, or null if none.
     * @return true if the action was successful, false otherwise
     */
    public abstract boolean run(BluetoothSocket socket);

    protected String getCreationTime() {
        return DateTimeUtils.formatDateTime(new Date(creationTimestamp));
    }

    public String toString() {
        return getCreationTime() + ": " + getClass().getSimpleName();
    }
}
