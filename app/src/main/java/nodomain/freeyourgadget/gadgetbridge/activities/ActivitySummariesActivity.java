/*  Copyright (C) 2017-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.adapter.ActivitySummariesAdapter;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;



public class ActivitySummariesActivity extends AbstractListActivity<BaseActivitySummary> {
    private static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesActivity.class);
    private GBDevice mGBDevice;
    private SwipeRefreshLayout swipeLayout;
    HashMap<String , Integer> activityKindMap = new HashMap<>(1);
    int activityFilter=0;
    long dateFromFilter=0;
    long dateToFilter=0;
    String nameContainsFilter;
    boolean offscreen = true;
    static final int ACTIVITY_FILTER=1;
    static final int ACTIVITY_DETAIL=11;


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
            case android.R.id.home:
                // back button, close drawer if open, otherwise exit
                if (!offscreen){
                    processSummaryStatistics();
                }else{
                    finish();
                }
                return true;
            case R.id.activity_action_manage_timestamp:
                resetFetchTimestampToChosenDate();
                processed = true;
                break;
            case R.id.activity_action_filter:
                if (!offscreen) processSummaryStatistics(); //hide drawer with stats if shown
                Intent filterIntent = new Intent(this, ActivitySummariesFilter.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable("activityKindMap",activityKindMap);
                bundle.putInt("activityFilter",activityFilter);
                bundle.putLong("dateFromFilter",dateFromFilter);
                bundle.putLong("dateToFilter",dateToFilter);
                bundle.putString("nameContainsFilter",nameContainsFilter);
                filterIntent.putExtras(bundle);
                startActivityForResult(filterIntent,ACTIVITY_FILTER);
                return true;
            case R.id.activity_action_calculate_summary_stats:
                processSummaryStatistics();
                return true;
        }
        return processed;
    }

    private void processSummaryStatistics() {
        View hiddenLayout = findViewById(R.id.activity_summary_statistics_relative_layout);
        hiddenLayout.setVisibility(View.VISIBLE);
        //hide or show drawer
        int yOffset = (offscreen) ? 0 : -1 * hiddenLayout.getHeight();
        LinearLayout.LayoutParams rlParams =
                (LinearLayout.LayoutParams) hiddenLayout.getLayoutParams();
        rlParams.setMargins(0, yOffset, 0, 0);
        hiddenLayout.setLayoutParams(rlParams);

        Animation animFadeDown;
        animFadeDown = AnimationUtils.loadAnimation(
                this,
                R.anim.slidefromtop);
        setTitle(R.string.activity_summaries);
        if (offscreen) {
            setTitle(R.string.activity_summaries_statistics);
            hiddenLayout.startAnimation(animFadeDown);
            double durationSum = 0;
            double caloriesBurntSum = 0;
            double distanceSum = 0;
            double activeSecondsSum = 0;
            double firstItemDate = 0;
            double lastItemDate = 0;

            TextView durationSumView = findViewById(R.id.activity_stats_duration_sum_value);
            TextView caloriesBurntSumView = findViewById(R.id.activity_stats_calories_burnt_sum_value);
            TextView distanceSumView = findViewById(R.id.activity_stats_distance_sum_value);
            TextView activeSecondsSumView = findViewById(R.id.activity_stats_activeSeconds_sum_value);
            TextView timeStartView = findViewById(R.id.activity_stats_timeFrom_value);
            TextView timeEndView = findViewById(R.id.activity_stats_timeTo_value);

            for (BaseActivitySummary sportitem : getItemAdapter().getItems()) {

                if (firstItemDate == 0) firstItemDate = sportitem.getStartTime().getTime();
                lastItemDate = sportitem.getEndTime().getTime();
                durationSum += sportitem.getEndTime().getTime() - sportitem.getStartTime().getTime();

                ActivitySummaryJsonSummary activitySummaryJsonSummary = new ActivitySummaryJsonSummary(sportitem);
                JSONObject summarySubdata = activitySummaryJsonSummary.getSummaryData();

                if (summarySubdata != null) {
                    try {
                        if (summarySubdata.has("caloriesBurnt")) {
                            caloriesBurntSum += summarySubdata.getJSONObject("caloriesBurnt").getDouble("value");
                        }
                        if (summarySubdata.has("distanceMeters")) {
                            distanceSum += summarySubdata.getJSONObject("distanceMeters").getDouble("value");
                        }
                        if (summarySubdata.has("activeSeconds")) {
                            activeSecondsSum += summarySubdata.getJSONObject("activeSeconds").getDouble("value");
                        }
                    } catch (JSONException e) {
                        LOG.error("SportsActivity", e);
                    }
                }
            }
            DecimalFormat df = new DecimalFormat("#.##");
            durationSumView.setText(String.format("%s", DateTimeUtils.formatDurationHoursMinutes((long) durationSum, TimeUnit.MILLISECONDS)));
            caloriesBurntSumView.setText(String.format("%s %s", (long) caloriesBurntSum, getString(R.string.calories_unit)));
            distanceSumView.setText(String.format("%s %s", df.format(distanceSum / 1000), getString(R.string.km)));
            activeSecondsSumView.setText(String.format("%s", DateTimeUtils.formatDurationHoursMinutes((long) activeSecondsSum, TimeUnit.SECONDS)));

            //start and end are inverted when filer not applied, because items are sorted the other way
            timeStartView.setText((dateFromFilter != 0) ? DateTimeUtils.formatDate(new Date(dateFromFilter)) : DateTimeUtils.formatDate(new Date((long) lastItemDate)));
            timeEndView.setText((dateToFilter != 0) ? DateTimeUtils.formatDate(new Date(dateToFilter)) : DateTimeUtils.formatDate(new Date((long) firstItemDate)));
        }
        offscreen = !offscreen;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == ACTIVITY_FILTER && resultData != null) {
            activityFilter = resultData.getIntExtra("activityFilter", 0);
            dateFromFilter = resultData.getLongExtra("dateFromFilter", 0);
            dateToFilter = resultData.getLongExtra("dateToFilter", 0);
            nameContainsFilter = resultData.getStringExtra("nameContainsFilter");
            setActivityKindFilter(activityFilter);
            setDateFromFilter(dateFromFilter);
            setDateToFilter(dateToFilter);
            setNameContainsFilter(nameContainsFilter);
            refresh();
        }
        if (requestCode == ACTIVITY_DETAIL) {
            refresh();
        }

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
        ActivitySummariesAdapter activitySummariesAdapter = new ActivitySummariesAdapter(this, mGBDevice,activityFilter,dateFromFilter,dateToFilter,nameContainsFilter);
        int backgroundColor = getBackgroundColor(ActivitySummariesActivity.this);
        activitySummariesAdapter.setBackgroundColor(backgroundColor);
        setItemAdapter(activitySummariesAdapter);

        getItemListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item != null) {
                    ActivitySummary summary = (ActivitySummary) item;
                    try {
                        showActivityDetail(position);
                    } catch (Exception e) {
                        GB.toast(getApplicationContext(), "Unable to display Activity Detail, maybe the activity is not available yet: " + e.getMessage(), Toast.LENGTH_LONG, GB.ERROR, e);
                    }

                }
            }
        });

        getItemListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        //hide top drawer on init
        View hiddenLayout = findViewById(R.id.activity_summary_statistics_relative_layout);
        hiddenLayout.setVisibility(View.GONE);

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
                                    ActivitySummary summary = item;

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

        activityKindMap = fillKindMap();



    }

    private LinkedHashMap fillKindMap(){
        LinkedHashMap<String , Integer> newMap = new LinkedHashMap<>(1); //reset
        newMap.put(getString(R.string.activity_summaries_all_activities), 0);
        for (BaseActivitySummary item : getItemAdapter().getItems()) {
            String activityName = ActivityKind.asString(item.getActivityKind(), this);
            if (!newMap.containsKey(activityName)) {
                newMap.put(activityName, item.getActivityKind());
            }
        }
        return newMap;
    }

    public void resetFetchTimestampToChosenDate() {
        final Calendar currentDate = Calendar.getInstance();
        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar date = Calendar.getInstance();
                date.set(year, monthOfYear, dayOfMonth);

                long timestamp = date.getTimeInMillis() - 1000;
                SharedPreferences.Editor editor = GBApplication.getDeviceSpecificSharedPrefs(mGBDevice.getAddress()).edit();
                editor.remove("lastSportsActivityTimeMillis"); //FIXME: key reconstruction is BAD
                editor.putLong("lastSportsActivityTimeMillis", timestamp);
                editor.apply();
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

    private void showActivityDetail(int position){
        Intent ActivitySummaryDetailIntent = new Intent(this, ActivitySummaryDetail.class);
        ActivitySummaryDetailIntent.putExtra("position", position);
        ActivitySummaryDetailIntent.putExtra("filter", activityFilter);
        ActivitySummaryDetailIntent.putExtra("dateFromFilter",dateFromFilter);
        ActivitySummaryDetailIntent.putExtra("dateToFilter",dateToFilter);
        ActivitySummaryDetailIntent.putExtra("nameContainsFilter",nameContainsFilter);

        ActivitySummaryDetailIntent.putExtra(GBDevice.EXTRA_DEVICE, mGBDevice);
        startActivityForResult(ActivitySummaryDetailIntent,ACTIVITY_DETAIL);
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

    public static int getBackgroundColor(Context context) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(R.attr.sports_activity_summary_background, typedValue, true);
        return typedValue.data;
    }

}
