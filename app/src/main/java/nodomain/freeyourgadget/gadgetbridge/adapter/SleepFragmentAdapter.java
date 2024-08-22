package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.SleepChartFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.WeekSleepChartFragment;

public class SleepFragmentAdapter extends NestedFragmentAdapter {
    public SleepFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment, childFragmentManager);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new SleepChartFragment();
            case 1:
                return WeekSleepChartFragment.newInstance(7);
            case 2:
                return WeekSleepChartFragment.newInstance(30);
        }
        return new SleepChartFragment();
    }
}
