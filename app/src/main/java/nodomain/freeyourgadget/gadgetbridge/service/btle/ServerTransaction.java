/*  Copyright (C) 2015-2019 Andreas Böhler, Andreas Shimokawa, Carsten
    Pfeiffer, Daniele Gobbetti

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
import java.util.Locale;

import androidx.annotation.Nullable;

/**
 * Groups a bunch of {@link BtLEServerAction actions} together, making sure
 * that upon failure of one action, all subsequent actions are discarded.
 *
 * @author TREND
 */
public class ServerTransaction extends AbstractTransaction {
    private final List<BtLEServerAction> mActions = new ArrayList<>(4);
    private
    @Nullable
    GattServerCallback gattCallback;

    public ServerTransaction(String taskName) {
        super(taskName);
    }

    public void add(BtLEServerAction action) {
        mActions.add(action);
    }

    public List<BtLEServerAction> getActions() {
        return Collections.unmodifiableList(mActions);
    }

    public boolean isEmpty() {
        return mActions.isEmpty();
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s: Transaction task: %s with %d actions", getCreationTime(), getTaskName(), mActions.size());
    }

    public void setGattCallback(@Nullable GattServerCallback callback) {
        gattCallback = callback;
    }

    /**
     * Returns the GattServerCallback for this transaction, or null if none.
     */
    public
    @Nullable
    GattServerCallback getGattCallback() {
        return gattCallback;
    }

    @Override
    public int getActionCount() {
        return mActions.size();
    }
}
