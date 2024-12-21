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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import androidx.annotation.Nullable;

import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.WaitAction;
import nodomain.freeyourgadget.gadgetbridge.service.btbr.actions.WriteAction;

public class TransactionBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(TransactionBuilder.class);

    private final Transaction mTransaction;
    private boolean mQueued;

    public TransactionBuilder(String taskName) {
        mTransaction = new Transaction(taskName);
    }

    public TransactionBuilder write(byte[] data) {
        WriteAction action = new WriteAction(data);
        return add(action);
    }


    /**
     * Causes the queue to sleep for the specified time.
     * Note that this is usually a bad idea, since it will not be able to process messages
     * during that time. It is also likely to cause race conditions.
     * @param millis the number of milliseconds to sleep
     */
    public TransactionBuilder wait(int millis) {
        WaitAction action = new WaitAction(millis);
        return add(action);
    }

    public TransactionBuilder add(BtBRAction action) {
        mTransaction.add(action);
        return this;
    }

    /**
     * Sets a SocketCallback instance that will be called when the transaction is executed,
     * resulting in SocketCallback events.
     *
     * @param callback the callback to set, may be null
     */
    public void setCallback(@Nullable SocketCallback callback) {
        mTransaction.setCallback(callback);
    }

    /**
     * To be used as the final step to execute the transaction by the given queue.
     *
     * @param queue
     */
    public void queue(BtBRQueue queue) {
        if (mQueued) {
            throw new IllegalStateException("This builder had already been queued. You must not reuse it.");
        }
        mQueued = true;
        queue.add(mTransaction);
    }

    public Transaction getTransaction() {
        return mTransaction;
    }

}
