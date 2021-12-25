/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.widget.ListView;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AbstractActivityListingAdapter;

public abstract class AbstractListActivity<T> extends AbstractGBActivity {
    private AbstractActivityListingAdapter<T> itemAdapter;
    private ListView itemListView;

    public void setItemAdapter(AbstractActivityListingAdapter<T> itemAdapter) {
        this.itemAdapter = itemAdapter;
        itemListView.setAdapter(itemAdapter);
    }

    protected void refresh() {
        this.itemAdapter.loadItems();
    }

    public void setActivityKindFilter(int activityKind){
        this.itemAdapter.setActivityKindFilter(activityKind);
    }

    public void setDateFromFilter(long date){
        this.itemAdapter.setDateFromFilter(date);
    }

    public void setDateToFilter(long date){
        this.itemAdapter.setDateToFilter(date);
    }

    public void setNameContainsFilter(String name){
        this.itemAdapter.setNameContainsFilter(name);
    }

    public void setItemsFilter(List items) {
        this.itemAdapter.setItemsFilter(items);
    }

    public void setDeviceFilter(long device) {
        this.itemAdapter.setDeviceFilter(device);
    }

    public AbstractActivityListingAdapter<T> getItemAdapter() {
        return itemAdapter;
    }

    public ListView getItemListView() {
        return itemListView;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_list);
        itemListView = findViewById(R.id.itemListView);
    }
}
