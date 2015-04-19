package nodomain.freeyourgadget.gadgetbridge.btle;

import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public String toString() {
        return String.format(Locale.US, "Transaction task: %s with %d actions", getTaskName(), mActions.size());
    }
}
