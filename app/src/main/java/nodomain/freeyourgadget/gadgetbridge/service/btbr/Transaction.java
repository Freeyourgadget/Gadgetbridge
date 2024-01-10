/*  Copyright (C) 2022-2024 Damien Gaignon

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
package nodomain.freeyourgadget.gadgetbridge.service.btbr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * Groups a bunch of {@link BtBRAction actions} together, making sure
 * that upon failure of one action, all subsequent actions are discarded.
 *
 * @author TREND
 */
public class Transaction extends AbstractTransaction {
    private final List<BtBRAction> mActions = new ArrayList<>(4);
    private
    @Nullable
    SocketCallback socketCallback;

    public Transaction(String taskName) {
        super(taskName);
    }

    public void add(BtBRAction action) {
        mActions.add(action);
    }

    public List<BtBRAction> getActions() {
        return Collections.unmodifiableList(mActions);
    }

    public boolean isEmpty() {
        return mActions.isEmpty();
    }

    public void setCallback(@Nullable SocketCallback callback) {
        socketCallback = callback;
    }

    /**
     * Returns the GattCallback for this transaction, or null if none.
     */
    public
    @Nullable
    SocketCallback getSocketCallback() {
        return socketCallback;
    }

    @Override
    public int getActionCount() {
        return mActions.size();
    }
}
