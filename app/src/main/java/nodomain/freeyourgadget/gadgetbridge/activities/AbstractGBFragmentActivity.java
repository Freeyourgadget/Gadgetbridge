package nodomain.freeyourgadget.gadgetbridge.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * A base activity that supports paging through fragments by swiping.
 * Subclasses will have to add a ViewPager to their layout and add something
 * like this to hook it to the fragments:
 * <p/>
 * <pre>
 * // Set up the ViewPager with the sections adapter.
 * ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
 * viewPager.setAdapter(getPagerAdapter());
 * </pre>
 *
 * @see AbstractGBFragment
 */
public abstract class AbstractGBFragmentActivity extends FragmentActivity {
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private AbstractFragmentPagerAdapter mSectionsPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = createFragmentPagerAdapter(getSupportFragmentManager());
    }

    public AbstractFragmentPagerAdapter getPagerAdapter() {
        return mSectionsPagerAdapter;
    }

    /**
     * Creates a PagerAdapter that will create the fragments to be used with this
     * activity. The fragments should typically extend AbstractGBFragment
     *
     * @param fragmentManager
     * @return
     */
    protected abstract AbstractFragmentPagerAdapter createFragmentPagerAdapter(FragmentManager fragmentManager);
}
