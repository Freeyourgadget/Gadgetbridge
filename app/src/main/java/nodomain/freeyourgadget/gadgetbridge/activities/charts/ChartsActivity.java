package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractFragmentPagerAdapter;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragmentActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

public class ChartsActivity extends AbstractGBFragmentActivity implements ChartsHost {

    private static final Logger LOG = LoggerFactory.getLogger(ChartsActivity.class);

    private Button mPrevButton;
    private Button mNextButton;
    private TextView mDateControl;

    private Date mStartDate;
    private Date mEndDate;
    private SwipeRefreshLayout swipeLayout;
    private PagerTabStrip mPagerTabStrip;
    private ViewPager viewPager;

    private static class ShowDurationDialog extends Dialog {
        private final String mDuration;
        private TextView durationLabel;

        public ShowDurationDialog(String duration, Context context) {
            super(context);
            mDuration = duration;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_charts_durationdialog);

            durationLabel = (TextView) findViewById(R.id.charts_duration_label);
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

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ControlCenter.ACTION_QUIT:
                    finish();
                    break;
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
            if (wasBusy) {
                swipeLayout.setRefreshing(false);
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(REFRESH));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        initDates();

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ControlCenter.ACTION_QUIT);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mGBDevice = extras.getParcelable(GBDevice.EXTRA_DEVICE);
        } else {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.activity_swipe_layout);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                fetchActivityData();
            }
        });

        // Set up the ViewPager with the sections adapter.
        viewPager = (ViewPager) findViewById(R.id.charts_pager);
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

        dateBar = (ViewGroup) findViewById(R.id.charts_date_bar);
        mDateControl = (TextView) findViewById(R.id.charts_text_date);
        mDateControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String detailedDuration = formatDetailedDuration();
                new ShowDurationDialog(detailedDuration, ChartsActivity.this).show();
            }
        });

        mPrevButton = (Button) findViewById(R.id.charts_previous);
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePrevButtonClicked();
            }
        });
        mNextButton = (Button) findViewById(R.id.charts_next);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleNextButtonClicked();
            }
        });
        mPagerTabStrip = (PagerTabStrip) findViewById(R.id.charts_pagerTabStrip);

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.charts_main_layout);
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

    private void handleNextButtonClicked() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DATE_NEXT));
    }

    private void handlePrevButtonClicked() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DATE_PREV));
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.charts_fetch_activity_data:
                fetchActivityData();
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void enableSwipeRefresh(boolean enable) {
        swipeLayout.setEnabled(enable);
    }

    private void fetchActivityData() {
        if (getDevice().isInitialized()) {
            GBApplication.deviceService().onFetchActivityData();
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

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            switch (position) {
                case 0:
                    return new ActivitySleepChartFragment();
                case 1:
                    return new SleepChartFragment();
                case 2:
                    return new WeekStepsChartFragment();
                case 3:
                    return new LiveActivityFragment();

            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.activity_sleepchart_activity_and_sleep);
                case 1:
                    return getString(R.string.sleepchart_your_sleep);
                case 2:
                    return getString(R.string.weekstepschart_steps_a_week);
                case 3:
                    return getString(R.string.liveactivity_live_activity);
            }
            return super.getPageTitle(position);
        }
    }
}
