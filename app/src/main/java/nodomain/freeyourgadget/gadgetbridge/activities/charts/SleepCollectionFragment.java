package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.adapter.NestedFragmentAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.SleepFragmentAdapter;

public class SleepCollectionFragment extends AbstractCollectionFragment {
    public SleepCollectionFragment() {

    }

    public static SleepCollectionFragment newInstance(final boolean allowSwipe) {
        final SleepCollectionFragment fragment = new SleepCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public NestedFragmentAdapter getNestedFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        return new SleepFragmentAdapter(this, getChildFragmentManager());
    }
}
