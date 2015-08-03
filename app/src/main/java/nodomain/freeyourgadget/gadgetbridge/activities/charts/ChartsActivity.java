package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.activities.ControlCenter;
import nodomain.freeyourgadget.gadgetbridge.service.DeviceCommunicationService;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.R;

public class ChartsActivity extends FragmentActivity {

    private static final Logger LOG = LoggerFactory.getLogger(ChartsActivity.class);

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case ControlCenter.ACTION_QUIT:
                    finish();
                    break;
                case ControlCenter.ACTION_REFRESH_DEVICELIST:
                    break;
                case GBDevice.ACTION_DEVICE_CHANGED:
                    GBDevice dev = intent.getParcelableExtra(GBDevice.EXTRA_DEVICE);
                    refreshBusyState(dev);
                    break;
            }
        }
    };
    private ProgressBar mProgressBar;

    private void refreshBusyState(GBDevice dev) {
        if (dev.isBusy()) {
            mProgressBar.setVisibility(View.VISIBLE);
        } else {
            boolean wasBusy = mProgressBar.getVisibility() != View.GONE;
            if (wasBusy) {
                mProgressBar.setVisibility(View.GONE);
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(AbstractChartFragment.ACTION_REFRESH));
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_charts);

        IntentFilter filterLocal = new IntentFilter();
        filterLocal.addAction(ControlCenter.ACTION_QUIT);
        filterLocal.addAction(ControlCenter.ACTION_REFRESH_DEVICELIST);
        filterLocal.addAction(GBDevice.ACTION_DEVICE_CHANGED);
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, filterLocal);


        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mProgressBar = (ProgressBar) findViewById(R.id.charts_progress);
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
                Intent startIntent = new Intent(this, DeviceCommunicationService.class);
                startIntent.setAction(DeviceCommunicationService.ACTION_FETCH_ACTIVITY_DATA);
                startService(startIntent);
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

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

            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return "Today".toUpperCase(l);
                case 1:
                    return "Week".toUpperCase(l);
                case 2:
                    return "Month".toUpperCase(l);
            }
            return null;
        }
    }
}
