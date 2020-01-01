/*  Copyright (C) 2015-2019 Andreas Shimokawa, Carsten Pfeiffer, Daniele
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

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

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
public abstract class AbstractGBFragmentActivity extends AbstractGBActivity {
    /**
     * The {@link PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link FragmentStatePagerAdapter}.
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
