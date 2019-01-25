/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Lem Dulfo

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
import android.support.v4.app.NavUtils;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.AppBlacklistAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AppBlacklistActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(AppBlacklistActivity.class);

    private AppBlacklistAdapter appBlacklistAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appblacklist);
        RecyclerView appListView = (RecyclerView) findViewById(R.id.appListView);
        appListView.setLayoutManager(new LinearLayoutManager(this));

        appBlacklistAdapter = new AppBlacklistAdapter(R.layout.item_app_blacklist, this);

        appListView.setAdapter(appBlacklistAdapter);

        SearchView searchView = (SearchView) findViewById(R.id.appListViewSearch);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                appBlacklistAdapter.getFilter().filter(newText);
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_blacklist_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.blacklist_all_notif:
                appBlacklistAdapter.blacklistAllNotif();
                return true;
            case R.id.whitelist_all_notif:
                appBlacklistAdapter.whitelistAllNotif();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
