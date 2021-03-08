package nodomain.freeyourgadget.gadgetbridge.activities.charts;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesChartFragment;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySession;

public class ActivityListingDetail extends DialogFragment {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivityListingDetail.class);

    public ActivityListingDetail() {

    }

    public static ActivityListingDetail newInstance(int tsFrom, int tsTo, ActivitySession item, GBDevice device) {
        ActivityListingDetail frag = new ActivityListingDetail();
        Bundle args = new Bundle();
        args.putInt("tsFrom", tsFrom);
        args.putInt("tsTo", tsTo);
        args.putSerializable("item", item);
        args.putParcelable(GBDevice.EXTRA_DEVICE, device);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_list_detail, container);
    }

    @Override
    public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        int tsFrom = getArguments().getInt("tsFrom");
        int tsTo = getArguments().getInt("tsTo");
        ActivitySession item = (ActivitySession) getArguments().getSerializable("item");
        GBDevice gbDevice;
        gbDevice = getArguments().getParcelable(GBDevice.EXTRA_DEVICE);
        if (gbDevice == null) {
            throw new IllegalArgumentException("Must provide a device when invoking this activity");
        }

        final ActivitySummariesChartFragment activitySummariesChartFragment = new ActivitySummariesChartFragment();
        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.chartsFragmentHolder, activitySummariesChartFragment)
                .commit();
        activitySummariesChartFragment.setDateAndGetData(gbDevice, tsFrom, tsTo);

        ActivityListingAdapter stepListAdapter = new ActivityListingAdapter(getContext());
        View activityItem = view.findViewById(R.id.activityItemHolder);
        stepListAdapter.fill_item(item, 0, activityItem, null);
    }
}