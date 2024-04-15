/*  Copyright (C) 2024 Arjan Schrijver

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
package nodomain.freeyourgadget.gadgetbridge.activities.welcome;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBActivity;

public class WelcomeActivity extends AbstractGBActivity {
    private static final Logger LOG = LoggerFactory.getLogger(WelcomeActivity.class);

    private ViewPager2 viewPager;
    private WelcomeFragmentsPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AbstractGBActivity.init(this, AbstractGBActivity.NO_ACTIONBAR);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Configure ViewPager2 with fragment adapter and default fragment
        viewPager = findViewById(R.id.welcome_viewpager);
        pagerAdapter = new WelcomeFragmentsPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Set up welcome page indicator
        WelcomePageIndicator pageIndicator = findViewById(R.id.welcome_page_indicator);
        pageIndicator.setViewPager(viewPager);
    }

    private class WelcomeFragmentsPagerAdapter extends FragmentStateAdapter {
        public WelcomeFragmentsPagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new WelcomeFragmentIntro();
                case 1:
                    return new WelcomeFragmentOverview();
                case 2:
                    return new WelcomeFragmentDocsSource();
                case 3:
                    return new WelcomeFragmentPermissions();
                default:
                    return new WelcomeFragmentGetStarted();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
