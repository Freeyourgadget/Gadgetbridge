/*  Copyright (C) 2020-2024 Petr Vaněk

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
        // last one is empty to avoid items behind fab
        if (current_position + 2 < itemsAdapter.getItemCount()) {
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
