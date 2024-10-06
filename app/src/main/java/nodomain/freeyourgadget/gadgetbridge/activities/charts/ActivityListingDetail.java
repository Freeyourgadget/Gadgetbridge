/*  Copyright (C) 2021-2024 Petr Vaněk

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
import nodomain.freeyourgadget.gadgetbridge.model.ActivityListItem;
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
        activitySummariesChartFragment.setDateAndGetData(null, gbDevice, tsFrom, tsTo);

        ActivityListingAdapter stepListAdapter = new ActivityListingAdapter(getContext());
        View activityItem = view.findViewById(R.id.activityItemHolder);
        ActivityListItem activityListItem = new ActivityListItem(activityItem);
        activityListItem.update(
                item.getStartTime(),
                item.getEndTime(),
                item.getActivityKind(),
                null,
                item.getActiveSteps(),
                item.getDistance(),
                item.getHeartRateAverage(),
                item.getIntensity(),
                item.getEndTime().getTime() - item.getStartTime().getTime(),
                false,
                null,
                false,
                false
        );
    }
}
