/*  Copyright (C) 2015-2017 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;


public class AppBlacklistActivity extends GBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(AppBlacklistActivity.class);

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GBApplication.ACTION_QUIT)) {
                finish();
            }
        }
    };

    private IdentityHashMap<ApplicationInfo, String> nameMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appblacklist);

        final PackageManager pm = getPackageManager();

        final List<ApplicationInfo> packageList = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        ListView appListView = (ListView) findViewById(R.id.appListView);

        // sort the package list by label and blacklist status
        nameMap = new IdentityHashMap<>(packageList.size());
        for (ApplicationInfo ai : packageList) {
            CharSequence name = pm.getApplicationLabel(ai);
            if (name == null) {
                name = ai.packageName;
            }
            if (GBApplication.blacklist.contains(ai.packageName)) {
                // sort blacklisted first by prefixing with a '!'
                name = "!" + name;
            }
            nameMap.put(ai, name.toString());
        }

        Collections.sort(packageList, new Comparator<ApplicationInfo>() {
            @Override
            public int compare(ApplicationInfo ai1, ApplicationInfo ai2) {
                final String s1 = nameMap.get(ai1);
                final String s2 = nameMap.get(ai2);
                return s1.compareTo(s2);
            }
        });

        final ArrayAdapter<ApplicationInfo> adapter = new ArrayAdapter<ApplicationInfo>(this, R.layout.item_with_checkbox, packageList) {
            @Override
            public View getView(int position, View view, ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    view = inflater.inflate(R.layout.item_with_checkbox, parent, false);
                }

                ApplicationInfo appInfo = packageList.get(position);
                TextView deviceAppVersionAuthorLabel = (TextView) view.findViewById(R.id.item_details);
                TextView deviceAppNameLabel = (TextView) view.findViewById(R.id.item_name);
                ImageView deviceImageView = (ImageView) view.findViewById(R.id.item_image);
                CheckBox checkbox = (CheckBox) view.findViewById(R.id.item_checkbox);

                deviceAppVersionAuthorLabel.setText(appInfo.packageName);
                deviceAppNameLabel.setText(nameMap.get(appInfo));
                deviceImageView.setImageDrawable(appInfo.loadIcon(pm));

                checkbox.setChecked(GBApplication.blacklist.contains(appInfo.packageName));

                return view;
            }
        };
        appListView.setAdapter(adapter);

        appListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                String packageName = packageList.get(position).packageName;
                CheckBox checkBox = ((CheckBox) v.findViewById(R.id.item_checkbox));
                checkBox.toggle();
                if (checkBox.isChecked()) {
                    GBApplication.addToBlacklist(packageName);
                } else {
                    GBApplication.removeFromBlacklist(packageName);
                }
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(GBApplication.ACTION_QUIT);

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
