/*  Copyright (C) 2015-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, vanous, Vebryn

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractFragmentPagerAdapter;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragmentActivity;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.RecordedDataTypes;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.GB;
import nodomain.freeyourgadget.gadgetbridge.util.LimitedQueue;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class ChartsActivity extends AbstractGBFragmentActivity implements ChartsHost {
    private static final Logger LOG = LoggerFactory.getLogger(ChartsActivity.class);

    private TextView mDateControl;

    private Date mStartDate;
    private Date mEndDate;
    private SwipeRefreshLayout swipeLayout;

    LimitedQueue mActivityAmountCache = new LimitedQueue(60);
    List<String> enabledTabsList;

    public static class ShowDurationDialog extends Dialog {
        private final String mDuration;
        private TextView durationLabel;

        ShowDurationDialog(String duration, Context context) {
            super(context);
            mDuration = duration;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_charts_durationdialog);

            durationLabel = findViewById(R.id.charts_duration_label);
            setDuration(mDuration);
        }

        public void setDuration(String duration) {
            if (mDuration != null) {
                durationLabel.setText(duration);
            } else {
                durationLabel.setText("");
            }
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    refreshBusyState(dev);
                    break;
            }
        }
    };
    private GBDevice mGBDevice;
    private ViewGroup dateBar;

    private void refreshBusyState(GBDevice dev) {
        if (dev.isBusy()) {
            swipeLayout.setRefreshing(true);
        } else {
            boolean wasBusy = swipeLayout.isRefreshing();
            swipeLayout.setRefreshing(false);
            if (wasBusy) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REFRESH));
            }
        }
        enableSwipeRefresh(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        initDates();

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        Prefs prefs = new Prefs(GBApplication.getDeviceSpecificSharedPrefs(getDevice().getAddress()));
        String myTabs = prefs.getString("charts_tabs", null);

        if (myTabs == null) {
            //make list mutable to be able to remove items later
            enabledTabsList = new ArrayList<String>(Arrays.asList(this.getResources().getStringArray(R.array.pref_charts_tabs_items_default)));
        } else {
            enabledTabsList = new ArrayList<String>(Arrays.asList(myTabs.split(",")));
        }
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(mGBDevice);
        if (!coordinator.supportsRealtimeData()) {
            enabledTabsList.remove("livestats");
        }

        swipeLayout = findViewById(R.id.activity_swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchActivityData();
            }
        });
        enableSwipeRefresh(true);

        // Set up the ViewPager with the sections adapter.
        NonSwipeableViewPager viewPager = findViewById(R.id.charts_pager);
        viewPager.setAdapter(getPagerAdapter());
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                enableSwipeRefresh(state == ViewPager.SCROLL_STATE_IDLE);
            }
        });

        dateBar = findViewById(R.id.charts_date_bar);
        mDateControl = findViewById(R.id.charts_text_date);
        mDateControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String detailedDuration = formatDetailedDuration();
                new ShowDurationDialog(detailedDuration, ChartsActivity.this).show();
            }
        });

        Button mPrevButton = findViewById(R.id.charts_previous_day);
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClicked(DATE_PREV_DAY);
            }
        });
        Button mNextButton = findViewById(R.id.charts_next_day);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClicked(DATE_NEXT_DAY);
            }
        });

        Button mPrevWeekButton = findViewById(R.id.charts_previous_week);
        mPrevWeekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClicked(DATE_PREV_WEEK);
            }
        });
        Button mNextWeekButton = findViewById(R.id.charts_next_week);
        mNextWeekButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClicked(DATE_NEXT_WEEK);
            }
        });

        Button mPrevMonthButton = findViewById(R.id.charts_previous_month);
        mPrevMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClicked(DATE_PREV_MONTH);
            }
        });
        Button mNextMonthButton = findViewById(R.id.charts_next_month);
        mNextMonthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleButtonClicked(DATE_NEXT_MONTH);
            }
        });


    }

    private String formatDetailedDuration() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        String dateStringFrom = dateFormat.format(getStartDate());
        String dateStringTo = dateFormat.format(getEndDate());

        return getString(R.string.sleep_activity_date_range, dateStringFrom, dateStringTo);
    }

    protected void initDates() {
        setEndDate(new Date());
        setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));
    }

    @Override
    public GBDevice getDevice() {
        return mGBDevice;
    }

    @Override
    public void setStartDate(Date startDate) {
        mStartDate = startDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        mEndDate = endDate;
    }

    @Override
    public Date getStartDate() {
        return mStartDate;
    }

    @Override
    public Date getEndDate() {
        return mEndDate;
    }

    private void  handleButtonClicked(String Action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(Action));
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_charts, menu);

        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(mGBDevice);
        if (!mGBDevice.isConnected() || !coordinator.supportsActivityDataFetching()) {
            menu.removeItem(R.id.charts_fetch_activity_data);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            this.recreate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.charts_fetch_activity_data:
                fetchActivityData();
                return true;
            case R.id.prefs_charts_menu:
                Intent settingsIntent = new Intent(this, ChartsPreferencesActivity.class);
                startActivityForResult(settingsIntent,1);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void enableSwipeRefresh(boolean enable) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(mGBDevice);
        swipeLayout.setEnabled(enable && coordinator.allowFetchActivityData(mGBDevice));
    }

    private void fetchActivityData() {
        if (getDevice().isInitialized()) {
            GBApplication.deviceService().onFetchRecordedData(RecordedDataTypes.TYPE_ACTIVITY);
        } else {
            swipeLayout.setRefreshing(false);
            GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
        }
    }

    @Override
    public void setDateInfo(String dateInfo) {
        mDateControl.setText(dateInfo);
    }

    @Override
    protected AbstractFragmentPagerAdapter createFragmentPagerAdapter(FragmentManager fragmentManager) {
        return new SectionsPagerAdapter(fragmentManager);
    }

    @Override
    public ViewGroup getDateBar() {
        return dateBar;
    }




    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends AbstractFragmentPagerAdapter {
        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }


        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (enabledTabsList.get(position)) {
                case "activity":
                    return new ActivitySleepChartFragment();
                case "activitylist":
                    return new ActivityListingChartFragment();
                case "sleep":
                    return new SleepChartFragment();
                case "sleepweek":
                    return new WeekSleepChartFragment();
                case "stepsweek":
                    return new WeekStepsChartFragment();
                case "speedzones":
                    return new SpeedZonesFragment();
                case "livestats":
                    return new LiveActivityFragment();
            }
            return null;
        }

        @Override
        public int getCount() {
            return enabledTabsList.toArray().length;
        }

        private String getSleepTitle() {
            if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
                return getString(R.string.weeksleepchart_sleep_a_month);
            }
            else{
                return getString(R.string.weeksleepchart_sleep_a_week);
            }
        }

        public String getStepsTitle() {
            if (GBApplication.getPrefs().getBoolean("charts_range", true)) {
                return getString(R.string.weekstepschart_steps_a_month);
            }
            else{
                return getString(R.string.weekstepschart_steps_a_week);
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {

            switch (enabledTabsList.get(position)) {
                case "activity":
                    return getString(R.string.activity_sleepchart_activity_and_sleep);
                case "activitylist":
                    return getString(R.string.charts_activity_list);
                case "sleep":
                    return getString(R.string.sleepchart_your_sleep);
                case "sleepweek":
                    return getSleepTitle();
                case "stepsweek":
                    return getStepsTitle();
                case "speedzones":
                    return getString(R.string.stats_title);
                case "livestats":
                    return getString(R.string.liveactivity_live_activity);
            }
            return super.getPageTitle(position);
        }
    }
}

class NonSwipeableViewPager extends ViewPager {

    public NonSwipeableViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (GBApplication.getPrefs().getBoolean("charts_allow_swipe", true)) {
            return super.onInterceptTouchEvent(ev);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (GBApplication.getPrefs().getBoolean("charts_allow_swipe", true)) {
            return super.onTouchEvent(ev);
        }
        return false;
    }
}
