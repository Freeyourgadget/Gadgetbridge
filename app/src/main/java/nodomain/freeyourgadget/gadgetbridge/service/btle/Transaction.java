/*  Copyright (C) 2015-2024 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Damien Gaignon, Daniele Gobbetti, José Rebelo

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

    private boolean modifyGattCallback;

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

    public void setCallback(@Nullable GattCallback callback) {
        gattCallback = callback;
        modifyGattCallback = true;
    }

    /**
     * Returns the GattCallback for this transaction, or null if none.
     */
    public
    @Nullable
    GattCallback getGattCallback() {
        return gattCallback;
    }

    /**
     * Returns whether the gatt callback should be modified for this transaction (either set, or
     * unset if {@code getGattCallback} is null.
     */
    public boolean isModifyGattCallback() {
        return modifyGattCallback;
    }

    @Override
    public int getActionCount() {
        return mActions.size();
    }
}
