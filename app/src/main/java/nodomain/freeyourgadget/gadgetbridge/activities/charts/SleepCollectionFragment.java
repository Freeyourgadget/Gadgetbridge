package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.adapter.SleepFragmentAdapter;

public class SleepCollectionFragment extends AbstractGBFragment {
    protected SleepFragmentAdapter nestedFragmentsAdapter;
    protected ViewPager2 viewPager;
    private int last_position = 0;

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
        nestedFragmentsAdapter = new SleepFragmentAdapter(this, getChildFragmentManager());
        viewPager = rootView.findViewById(R.id.pager);
        viewPager.setAdapter(nestedFragmentsAdapter);
        viewPager.setOrientation(ViewPager2.ORIENTATION_VERTICAL);
        viewPager.setUserInputEnabled(false);
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

