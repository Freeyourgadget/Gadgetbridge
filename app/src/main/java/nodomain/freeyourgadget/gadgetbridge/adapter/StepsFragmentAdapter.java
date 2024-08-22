package nodomain.freeyourgadget.gadgetbridge.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepsDailyFragment;
import nodomain.freeyourgadget.gadgetbridge.activities.charts.StepsPeriodFragment;

public class StepsFragmentAdapter extends NestedFragmentAdapter {
    protected FragmentManager fragmentManager;

    public StepsFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        super(fragment, childFragmentManager);
        fragmentManager = childFragmentManager;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new StepsDailyFragment();
            case 1:
                return StepsPeriodFragment.newInstance(7);
            case 2:
                return StepsPeriodFragment.newInstance(30);
        }
        return new StepsDailyFragment();
    }
}
