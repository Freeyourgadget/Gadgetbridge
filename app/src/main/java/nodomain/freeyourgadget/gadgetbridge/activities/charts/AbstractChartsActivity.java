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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragmentActivity;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public abstract class AbstractChartsActivity extends AbstractGBFragmentActivity implements ChartsHost {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractChartsActivity.class);

    public static final String STATE_START_DATE = "stateStartDate";
    public static final String STATE_END_DATE = "stateEndDate";

    public static final String EXTRA_FRAGMENT_ID = "fragment";
    public static final int REQUEST_CODE_PREFERENCES = 1;

    private TextView mDateControl;

    private Date mStartDate;
    private Date mEndDate;
    private SwipeRefreshLayout swipeLayout;

    List<String> enabledTabsList;

    private GBDevice mGBDevice;
    private ViewGroup dateBar;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null) {
                        refreshBusyState(dev);
                    }
                    break;
            }
        }
    };

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
        int tabFragmentToOpen = -1;

        if (savedInstanceState != null) {
            setEndDate(new Date(savedInstanceState.getLong(STATE_END_DATE, System.currentTimeMillis())));
            setStartDate(new Date(savedInstanceState.getLong(STATE_START_DATE, DateTimeUtils.shiftByDays(getEndDate(), -1).getTime())));
        } else {
            setEndDate(new Date());
            setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));
        }

        final IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        final Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
            tabFragmentToOpen = extras.getInt(EXTRA_FRAGMENT_ID);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }
        enabledTabsList = fillChartsTabsList();

        swipeLayout = findViewById(R.id.activity_swipe_layout);
        swipeLayout.setOnRefreshListener(this::fetchRecordedData);
        enableSwipeRefresh(true);

        // Set up the ViewPager with the sections adapter.
        final NonSwipeableViewPager viewPager = findViewById(R.id.charts_pager);
        viewPager.setAdapter(getPagerAdapter());
        if (tabFragmentToOpen > -1) {
            viewPager.setCurrentItem(tabFragmentToOpen);  // open the tab as specified in the intent
        }

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
        mDateControl.setOnClickListener(v -> {
            String detailedDuration = formatDetailedDuration();
            new ShowDurationDialog(detailedDuration, AbstractChartsActivity.this).show();
        });

        Button mPrevButton = findViewById(R.id.charts_previous_day);
        mPrevButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_DAY));
        Button mNextButton = findViewById(R.id.charts_next_day);
        mNextButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_DAY));

        Button mPrevWeekButton = findViewById(R.id.charts_previous_week);
        mPrevWeekButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_WEEK));
        Button mNextWeekButton = findViewById(R.id.charts_next_week);
        mNextWeekButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_WEEK));

        Button mPrevMonthButton = findViewById(R.id.charts_previous_month);
        mPrevMonthButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_MONTH));
        Button mNextMonthButton = findViewById(R.id.charts_next_month);
        mNextMonthButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_MONTH));
    }

    @Override
    protected void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putLong(STATE_END_DATE, getEndDate().getTime());
        outState.putLong(STATE_START_DATE, getStartDate().getTime());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        setEndDate(new Date(savedInstanceState.getLong(STATE_END_DATE, System.currentTimeMillis())));
        setStartDate(new Date(savedInstanceState.getLong(STATE_START_DATE, DateTimeUtils.shiftByDays(getEndDate(), -1).getTime())));
    }

    protected abstract List<String> fillChartsTabsList();

    private String formatDetailedDuration() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
        final String dateStringFrom = dateFormat.format(getStartDate());
        final String dateStringTo = dateFormat.format(getEndDate());

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

    @Override
    public void setDateInfo(final String dateInfo) {
        mDateControl.setText(dateInfo);
    }

    @Override
    public ViewGroup getDateBar() {
        return dateBar;
    }

    private void handleButtonClicked(final String action) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(action));
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

        if (!mGBDevice.isConnected() || !supportsRefresh()) {
            menu.removeItem(R.id.charts_fetch_activity_data);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PREFERENCES) {
            this.recreate();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.charts_fetch_activity_data:
                fetchRecordedData();
                return true;
            case R.id.prefs_charts_menu:
                Intent settingsIntent = new Intent(this, ChartsPreferencesActivity.class);
                startActivityForResult(settingsIntent, REQUEST_CODE_PREFERENCES);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void enableSwipeRefresh(boolean enable) {
        swipeLayout.setEnabled(enable && allowRefresh());
    }

    protected abstract boolean supportsRefresh();

    protected abstract boolean allowRefresh();

    protected abstract int getRecordedDataType();

    private void fetchRecordedData() {
        if (getDevice().isInitialized()) {
            GBApplication.deviceService(getDevice()).onFetchRecordedData(getRecordedDataType());
        } else {
            swipeLayout.setRefreshing(false);
            GB.toast(this, getString(R.string.device_not_connected), Toast.LENGTH_SHORT, GB.ERROR);
        }
    }
}
