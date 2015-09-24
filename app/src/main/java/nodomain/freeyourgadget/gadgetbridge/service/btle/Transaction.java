package nodomain.freeyourgadget.gadgetbridge.service.btle;

import android.support.annotation.Nullable;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Groups a bunch of {@link BtLEAction actions} together, making sure
 * that upon failure of one action, all subsequent actions are discarded.
 *
 * @author TREND
 */
public class Transaction {
    private String mName;
    private List<BtLEAction> mActions = new ArrayList<>(4);
    private long creationTimestamp = System.currentTimeMillis();
    private
    @Nullable
    GattCallback gattCallback;

    public Transaction(String taskName) {
        this.mName = taskName;
    }

    public String getTaskName() {
        return mName;
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

    protected String getCreationTime() {
        return DateFormat.getTimeInstance(DateFormat.MEDIUM).format(new Date(creationTimestamp));
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s: Transaction task: %s with %d actions", getCreationTime(), getTaskName(), mActions.size());
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
}
