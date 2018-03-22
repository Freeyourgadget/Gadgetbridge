/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A special action that checks for an abort-condition, and if met, the currently
 * executing transaction will be aborted by returning false.
 */
public abstract class AbortTransactionAction extends PlainAction {
    private static final Logger LOG = LoggerFactory.getLogger(AbortTransactionAction.class);

    public AbortTransactionAction() {
    }

    @Override
    public boolean run(BluetoothGatt gatt) {
        if (shouldAbort()) {
            LOG.info("Aborting transaction because abort criteria met.");
            return false;
        }
        return true;
    }

    protected abstract boolean shouldAbort();

    @Override
    public String toString() {
        return getCreationTime() + ": " + getClass().getSimpleName() + ": aborting? " + shouldAbort();
    }
}
