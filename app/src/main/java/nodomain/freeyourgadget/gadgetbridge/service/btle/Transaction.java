/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.btle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Groups a bunch of {@link BtLEAction actions} together, making sure
 * that upon failure of one action, all subsequent actions are discarded.
 *
 * @author TREND
 */
public class Transaction extends AbstractTransaction {
    private final List<BtLEAction> mActions = new ArrayList<>(4);
    private
    @Nullable
    GattCallback gattCallback;

    public Transaction(String taskName) {
        super(taskName);
    }

    public void add(BtLEAction action) {
        mActions.add(action);
    }

    public List<BtLEAction> getActions() {
        return Collections.unmodifiableList(mActions);
    }

    public boolean isEmpty() {
        return mActions.isEmpty();
    }

    public void setGattCallback(@Nullable GattCallback callback) {
        gattCallback = callback;
    }

    /**
     * Returns the GattCallback for this transaction, or null if none.
     */
    public
    @Nullable
    GattCallback getGattCallback() {
        return gattCallback;
    }

    @Override
    public int getActionSize() {
        return mActions.size();
    }
}
