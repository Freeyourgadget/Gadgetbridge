package nodomain.freeyourgadget.gadgetbridge.model;

import android.content.Context;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.adapter.ActivitySummariesAdapter;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class ActivitySummaryItems {
    private final GBDevice device;
    private int activityKindFilter;
    List<BaseActivitySummary> allItems;
    ActivitySummariesAdapter itemsAdapter;
    private int current_position = 0;
    long dateFromFilter=0;
    long dateToFilter=0;


    public ActivitySummaryItems(Context context, GBDevice device, int activityKindFilter, long dateFromFilter, long dateToFilter, String nameContainsFilter) {
        this.device = device;
        this.activityKindFilter = activityKindFilter;
        this.dateFromFilter=dateFromFilter;
        this.dateToFilter=dateToFilter;
        this.itemsAdapter = new ActivitySummariesAdapter(context, device, activityKindFilter, dateFromFilter, dateToFilter, nameContainsFilter);
    }

    public BaseActivitySummary getItem(int position){
        current_position=position;
        return itemsAdapter.getItem(position);
    }

    public int getPosition(BaseActivitySummary item){
        return itemsAdapter.getPosition(item);
    }

    public List<BaseActivitySummary> getAllItems(){
        return itemsAdapter.getItems();
    }

    public BaseActivitySummary getNextItem(){
        if (current_position+1 < itemsAdapter.getCount()){
            current_position+=1;
            return itemsAdapter.getItem(current_position);
        }
        return null;
    }

    public BaseActivitySummary getPrevItem(){
        if (current_position-1 >= 0){
            current_position-=1;
            return itemsAdapter.getItem(current_position);
        }
        return null;
    }

    public int getCurrent_position(){
        return current_position;
    }

}
