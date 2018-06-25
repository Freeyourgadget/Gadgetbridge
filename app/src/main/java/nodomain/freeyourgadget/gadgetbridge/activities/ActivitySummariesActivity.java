/*  Copyright (C) 2017-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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

import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ActivitySummariesAdapter;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.AndroidUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ActivitySummariesActivity extends AbstractListActivity<BaseActivitySummary> {

    private GBDevice mGBDevice;
    private SwipeRefreshLayout swipeLayout;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice device = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    mGBDevice = device;
                    if (device.isBusy()) {
                        swipeLayout.setRefreshing(true);
                    } else {
                        boolean wasBusy = swipeLayout.isRefreshing();
                        swipeLayout.setRefreshing(false);
                        if (wasBusy) {
                            refresh();
                        }
                    }
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.activity_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean processed = false;
        switch (item.getItemId()) {
            case R.id.activity_action_manage_timestamp:
                resetFetchTimestampToChosenDate();
                processed = true;
                break;
        }
        return processed;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        super.onCreate(savedInstanceState);
        setItemAdapter(new ActivitySummariesAdapter(this, mGBDevice));

        getItemListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item != null) {
                    ActivitySummary summary = (ActivitySummary) item;

                    String gpxTrack = summary.getGpxTrack();
                    if (gpxTrack != null) {
                        showTrack(gpxTrack);
                    } else {
                        GB.toast("This activity does not contain GPX tracks.", Toast.LENGTH_LONG, GB.INFO);
                    }
                }
            }
        });

        getItemListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        getItemListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
                final int selectedItems = getItemListView().getCheckedItemCount();
                actionMode.setTitle(selectedItems + " selected");
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                getMenuInflater().inflate(R.menu.activity_list_context_menu, menu);
                findViewById(R.id.fab).setVisibility(View.INVISIBLE);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                boolean processed = false;
                SparseBooleanArray checked = getItemListView().getCheckedItemPositions();
                switch (menuItem.getItemId()) {
                    case R.id.activity_action_delete:
                        List<BaseActivitySummary> toDelete = new ArrayList<>();
                        for(int i = 0; i<  checked.size(); i++) {
                            if (checked.valueAt(i)) {
                                toDelete.add(getItemAdapter().getItem(checked.keyAt(i)));
                            }
                        }
                        deleteItems(toDelete);
                        processed =  true;
                        break;
                    case R.id.activity_action_export:
                        List<String> paths = new ArrayList<>();


                        for(int i = 0; i<  checked.size(); i++) {
                            if (checked.valueAt(i)) {

                                BaseActivitySummary item = getItemAdapter().getItem(checked.keyAt(i));
                                if (item != null) {
                                    ActivitySummary summary = (ActivitySummary) item;

                                    String gpxTrack = summary.getGpxTrack();
                                    if (gpxTrack != null) {
                                        paths.add(gpxTrack);
                                    }
                                }
                            }
                        }
                        shareMultiple(paths);
                        processed = true;
                        break;
                    case R.id.activity_action_select_all:
                        for ( int i=0; i < getItemListView().getCount(); i++) {
                            getItemListView().setItemChecked(i, true);
                        }
                        return true; //don't finish actionmode in this case!
                    default:
                        break;
                }
                actionMode.finish();
                return processed;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
                findViewById(R.id.fab).setVisibility(View.VISIBLE);
            }
        });

        swipeLayout = findViewById(R.id.list_activity_swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchTrackData();
            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchTrackData();
            }
        });
    }

    public void resetFetchTimestampToChosenDate() {
        final Calendar currentDate = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);

                Long timestamp = date.getTimeInMillis() - 1000;
                SharedPreferences.Editor editor = GBApplication.getPrefs().getPreferences().edit();
                editor.remove(mGBDevice.getAddress() + "_" + "lastSportsActivityTimeMillis"); //FIXME: key reconstruction is BAD
                editor.putLong(mGBDevice.getAddress() + "_" + "lastSportsActivityTimeMillis", timestamp);
                editor.commit();
            }
        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    private void deleteItems(List<BaseActivitySummary> items) {
        for(BaseActivitySummary item : items) {
            item.delete();
            getItemAdapter().remove(item);
        }
        refresh();
    }

    private void showTrack(String gpxTrack) {
        try {
            AndroidUtils.viewFile(gpxTrack, Intent.ACTION_VIEW, this);
        } catch (IOException e) {
            GB.toast(this, "Unable to display GPX track: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
        }
    }

    private void fetchTrackData() {
        if (mGBDevice.isInitialized() && !mGBDevice.isBusy()) {
            GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_GPS_TRACKS);
        } else {
            swipeLayout.setRefreshing(false);
            if (!mGBDevice.isInitialized()) {
                GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
            }
        }
    }

    private void shareMultiple(List<String> paths){

        ArrayList<Uri> uris = new ArrayList<>();
        for(String path: paths){
            File file = new File(path);
            uris.add(FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".screenshot_provider", file));
        }

        if(uris.size() > 0) {
            final Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            intent.setType("application/gpx+xml");
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            startActivity(Intent.createChooser(intent, "SHARE"));
        } else {
            GB.toast(this, "No selected activity contains a GPX track to share", Toast.LENGTH_SHORT, GB.ERROR);
        }

    }
}
