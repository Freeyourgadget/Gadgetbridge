package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.adapter.NestedFragmentAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.StepsFragmentAdapter;

public class StepsCollectionFragment extends AbstractCollectionFragment {
    public StepsCollectionFragment() {

    }

    public static StepsCollectionFragment newInstance(final boolean allowSwipe) {
        final StepsCollectionFragment fragment = new StepsCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public NestedFragmentAdapter getNestedFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        return new StepsFragmentAdapter(this, getChildFragmentManager());
    }
}

