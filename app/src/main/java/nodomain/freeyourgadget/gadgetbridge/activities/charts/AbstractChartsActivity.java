/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Activity;
import android.app.DatePickerDialog;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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

    public static final String EXTRA_FRAGMENT_ID = "fragmentId";
    public static final String EXTRA_SINGLE_FRAGMENT_NAME = "singleFragmentName";
    public static final String EXTRA_ACTIONBAR_TITLE = "actionbarTitle";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_MODE = "mode";

    private TextView mDateControl;

    private Date mStartDate;
    private Date mEndDate;
    private SwipeRefreshLayout swipeLayout;

    List<String> enabledTabsList;

    private GBDevice mGBDevice;
    private ViewGroup dateBar;

    private ActivityResultLauncher<Intent> chartsPreferencesLauncher;
    private final ActivityResultCallback<ActivityResult> chartsPreferencesCallback = result -> {
        recreate();
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            //noinspection SwitchStatementWithTooFewBranches
            switch (Objects.requireNonNull(action)) {
                case GBDevice.ACTION_DEVICE_CHANGED:
                    final GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    if (dev != null) {
                        refreshBusyState(dev);
                    }
                    break;
            }
        }
    };

    private void refreshBusyState(final GBDevice dev) {
        if (dev.isBusy()) {
            swipeLayout.setRefreshing(true);
        } else {
            final boolean wasBusy = swipeLayout.isRefreshing();
            swipeLayout.setRefreshing(false);
            if (wasBusy) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REFRESH));
            }
        }
        enableSwipeRefresh(true);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        final Bundle extras = getIntent().getExtras();
        if (extras == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);

        chartsPreferencesLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                chartsPreferencesCallback
        );

        // Set start and end date
        if (savedInstanceState != null) {
            setEndDate(new Date(savedInstanceState.getLong(STATE_END_DATE, System.currentTimeMillis())));
        } else if (extras.containsKey(EXTRA_TIMESTAMP)) {
            final int endTimestamp = extras.getInt(EXTRA_TIMESTAMP, 0);
            setEndDate(new Date(endTimestamp * 1000L));
        } else {
            setEndDate(new Date());
        }
        setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));

        final IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        // Open the specified fragment, if any, and setup single page view if specified
        final int tabFragmentIdToOpen = extras.getInt(EXTRA_FRAGMENT_ID, -1);
        final String singleFragmentName = extras.getString(EXTRA_SINGLE_FRAGMENT_NAME, null);
        final int actionbarTitle = extras.getInt(EXTRA_ACTIONBAR_TITLE, 0);

        if (tabFragmentIdToOpen >= 0 && singleFragmentName != null) {
            throw new IllegalArgumentException("Must specify either fragment ID or single fragment name, not both");
        }

        if (singleFragmentName != null) {
            enabledTabsList = Collections.singletonList(singleFragmentName);
        } else {
            enabledTabsList = fillChartsTabsList();
        }

        swipeLayout = findViewById(R.id.activity_swipe_layout);
        swipeLayout.setOnRefreshListener(this::fetchRecordedData);
        enableSwipeRefresh(true);

        // Set up the ViewPager with the sections adapter.
        final NonSwipeableViewPager viewPager = findViewById(R.id.charts_pager);
        viewPager.setAdapter(getPagerAdapter());
        if (tabFragmentIdToOpen > -1) {
            viewPager.setCurrentItem(tabFragmentIdToOpen);  // open the tab as specified in the intent
        }

        viewPager.setAllowSwipe(singleFragmentName == null && GBApplication.getPrefs().getBoolean("charts_allow_swipe", true));

        if (singleFragmentName != null) {
            final TabLayout tabLayout = findViewById(R.id.charts_pagerTabStrip);
            tabLayout.setVisibility(TextView.GONE);
        }

        if (actionbarTitle != 0) {
            final ActionBar actionBar = getSupportActionBar();

            if (actionBar != null) {
                actionBar.setTitle(actionbarTitle);
            }
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

        final Button mPrevButton = findViewById(R.id.charts_previous_day);
        mPrevButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_DAY));
        final Button mNextButton = findViewById(R.id.charts_next_day);
        mNextButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_DAY));

        final Button mPrevWeekButton = findViewById(R.id.charts_previous_week);
        mPrevWeekButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_WEEK));
        final Button mNextWeekButton = findViewById(R.id.charts_next_week);
        mNextWeekButton.setOnClickListener(v -> handleButtonClicked(DATE_NEXT_WEEK));

        final Button mPrevMonthButton = findViewById(R.id.charts_previous_month);
        mPrevMonthButton.setOnClickListener(v -> handleButtonClicked(DATE_PREV_MONTH));
        final Button mNextMonthButton = findViewById(R.id.charts_next_month);
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
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.charts_fetch_activity_data) {
            fetchRecordedData();
            return true;
        } else if (itemId == R.id.charts_set_date) {
            final Calendar currentDate = Calendar.getInstance();
            currentDate.setTime(getEndDate());
            new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
                currentDate.set(year, monthOfYear, dayOfMonth);
                setEndDate(currentDate.getTime());
                setStartDate(DateTimeUtils.shiftByDays(getEndDate(), -1));
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REFRESH));
            }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DATE)).show();
        } else if (itemId == R.id.prefs_charts_menu) {
            final Intent settingsIntent = new Intent(this, ChartsPreferencesActivity.class);
            chartsPreferencesLauncher.launch(settingsIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void enableSwipeRefresh(final boolean enable) {
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
