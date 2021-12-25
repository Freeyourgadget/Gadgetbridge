package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.adapter.ActivitySummariesAdapter;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ActivitySummaryItems {
    ActivitySummariesAdapter itemsAdapter;
    private int current_position = 0;

    public ActivitySummaryItems(Context context, GBDevice device, int activityKindFilter, long dateFromFilter, long dateToFilter, String nameContainsFilter, long deviceFilter, List itemsFilter) {

        this.itemsAdapter = new ActivitySummariesAdapter(context, device, activityKindFilter, dateFromFilter, dateToFilter, nameContainsFilter, deviceFilter, itemsFilter);
    }

    public BaseActivitySummary getItem(int position) {
        if (position == 0) return null;
        current_position = position;
        return itemsAdapter.getItem(position);
    }

    public int getPosition(BaseActivitySummary item) {
        return itemsAdapter.getPosition(item);
    }

    public BaseActivitySummary getNextItem() {
        if (current_position + 1 < itemsAdapter.getCount()) {
            current_position += 1;
            return itemsAdapter.getItem(current_position);
        }
        return null;
    }

    public BaseActivitySummary getPrevItem() {
        if (current_position - 1 >= 1) { //0 is empty item for summary dashboard
            current_position -= 1;
            return itemsAdapter.getItem(current_position);
        }
        return null;
    }
}
