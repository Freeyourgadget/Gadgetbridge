package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import nodomain.freeyourgadget.gadgetbridge.activities.AbstractGBFragment;
import nodomain.freeyourgadget.gadgetbridge.adapter.NestedFragmentAdapter;
import nodomain.freeyourgadget.gadgetbridge.adapter.RespiratoryRateFragmentAdapter;

public class RespiratoryRateCollectionFragment extends AbstractCollectionFragment {
    public RespiratoryRateCollectionFragment() {

    }

    public static RespiratoryRateCollectionFragment newInstance(final boolean allowSwipe) {
        final RespiratoryRateCollectionFragment fragment = new RespiratoryRateCollectionFragment();
        final Bundle args = new Bundle();
        args.putBoolean(ARG_ALLOW_SWIPE, allowSwipe);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public NestedFragmentAdapter getNestedFragmentAdapter(AbstractGBFragment fragment, FragmentManager childFragmentManager) {
        return new RespiratoryRateFragmentAdapter(this, getChildFragmentManager());
    }
}

