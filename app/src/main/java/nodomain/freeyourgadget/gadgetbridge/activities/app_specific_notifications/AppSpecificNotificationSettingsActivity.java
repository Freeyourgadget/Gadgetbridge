/*  Copyright (C) 2023-2024 akasaka / Genjitsu Labs

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
package nodomain.freeyourgadget.gadgetbridge.activities.app_specific_notifications;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;
import nodomain.freeyourgadget.gadgetbridge.adapter.AppSpecificNotificationSettingsAppListAdapter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;

public class AppSpecificNotificationSettingsActivity extends AbstractGBActivity {

    private static final Logger LOG = LoggerFactory.getLogger(AppSpecificNotificationSettingsActivity.class);

    private AppSpecificNotificationSettingsAppListAdapter appListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_specific_notification_settings);
        RecyclerView appListView = findViewById(R.id.appListView);
        appListView.setLayoutManager(new LinearLayoutManager(this));

        GBDevice device = getIntent().getParcelableExtra(GBDevice.EXTRA_DEVICE);
        appListAdapter = new AppSpecificNotificationSettingsAppListAdapter(R.layout.item_app_specific_notification_app_list, this, device);

        appListView.setAdapter(appListAdapter);

        SearchView searchView = findViewById(R.id.appListViewSearch);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                appListAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
