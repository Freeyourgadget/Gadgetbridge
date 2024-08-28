/*  Copyright (C) 2024 a0z, José Rebelo

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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.adapter.NestedFragmentAdapter;

public abstract class AbstractCollectionFragment extends AbstractGBFragment {
    protected static final String ARG_ALLOW_SWIPE = "allow_swipe";

    protected NestedFragmentAdapter nestedFragmentsAdapter;
    protected ViewPager2 viewPager;
    private int last_position = 0;
    private boolean allowSwipe;

    public abstract NestedFragmentAdapter getNestedFragmentAdapter(final AbstractGBFragment fragment,
                                                                   final FragmentManager childFragmentManager);

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            allowSwipe = getArguments().getBoolean(ARG_ALLOW_SWIPE, false);
        }
    }

    @Override
    protected void onMadeVisibleInActivity() {
        super.onMadeVisibleInActivity();
        nestedFragmentsAdapter.updateFragments(last_position);
    }

    @Override
    public void onMadeInvisibleInActivity() {
        if (nestedFragmentsAdapter != null) {
            nestedFragmentsAdapter.updateFragments(-1);
        }
    }

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_nested_tabs, container, false);
        nestedFragmentsAdapter = getNestedFragmentAdapter(this, getChildFragmentManager());
        viewPager = rootView.findViewById(R.id.pager);
        viewPager.setAdapter(nestedFragmentsAdapter);
        if (!allowSwipe) {
            viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
            viewPager.setUserInputEnabled(false);
        }
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                last_position = position;
                viewPager.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isVisibleInActivity()) {
                            nestedFragmentsAdapter.updateFragments(position);
                        }
                    }
                });
            }
        });

        return rootView;
    }

    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(getString(R.string.calendar_day));
                    break;
                case 1:
                    tab.setText(getString(R.string.calendar_week));
                    break;
                case 2:
                    tab.setText(getString(R.string.calendar_month));
                    break;
            }
        }).attach();
    }

    @Nullable
    @Override
    protected CharSequence getTitle() {
        return null;
    }
}

