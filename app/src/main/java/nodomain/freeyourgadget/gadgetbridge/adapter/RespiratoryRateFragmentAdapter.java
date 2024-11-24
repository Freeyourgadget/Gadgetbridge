package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.RespiratoryRateDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.RespiratoryRatePeriodFragment;

public class RespiratoryRateFragmentAdapter extends NestedFragmentAdapter {
    protected FragmentManager fragmentManager;

    public RespiratoryRateFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment, childFragmentManager);
        fragmentManager = childFragmentManager;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new RespiratoryRateDailyFragment();
            case 1:
                return RespiratoryRatePeriodFragment.newInstance(7);
            case 2:
                return RespiratoryRatePeriodFragment.newInstance(30);
        }
        return new RespiratoryRateDailyFragment();
    }
}
