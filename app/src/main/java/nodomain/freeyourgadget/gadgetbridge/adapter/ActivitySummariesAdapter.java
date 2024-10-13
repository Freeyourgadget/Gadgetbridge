/*  Copyright (C) 2017-2024 Carsten Pfeiffer, Daniel Dakhno, Daniele Gobbetti,
    José Rebelo, Petr Vaněk

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
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityListItem;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryData;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryParser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesFilter.ALL_DEVICES;
import static nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryEntries.INTERNAL_HAS_GPS;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ActivitySummariesAdapter extends AbstractActivityListingAdapter<BaseActivitySummary> {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesAdapter.class);
    private final GBDevice device;
    long dateFromFilter = 0;
    long dateToFilter = 0;
    long deviceFilter;
    String nameContainsFilter;
    List<Long> itemsFilter;
    private int activityKindFilter;

    public ActivitySummariesAdapter(Context context, GBDevice device, int activityKindFilter, long dateFromFilter, long dateToFilter, String nameContainsFilter, long deviceFilter, List itemsFilter) {
        super(context);
        this.device = device;
        this.activityKindFilter = activityKindFilter;
        this.dateFromFilter = dateFromFilter;
        this.dateToFilter = dateToFilter;
        this.nameContainsFilter = nameContainsFilter;
        this.deviceFilter = deviceFilter;
        this.itemsFilter = itemsFilter;
        loadItems();
    }

    @Override
    public void loadItems() {
        try (DBHandler handler = GBApplication.acquireDB()) {
            BaseActivitySummaryDao summaryDao = handler.getDaoSession().getBaseActivitySummaryDao();
            Device dbDevice = DBHelper.findDevice(device, handler.getDaoSession());

            QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();

            if (deviceFilter == ALL_DEVICES) {
                qb.orderDesc(BaseActivitySummaryDao.Properties.StartTime);
            } else if (deviceFilter != 0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.DeviceId.eq(
                                deviceFilter)).orderDesc(BaseActivitySummaryDao.Properties.StartTime);
            } else {
                qb.where(
                        BaseActivitySummaryDao.Properties.DeviceId.eq(
                                dbDevice.getId())).orderDesc(BaseActivitySummaryDao.Properties.StartTime);
            }

            if (activityKindFilter != 0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.ActivityKind.eq(activityKindFilter));
            }

            if (dateFromFilter != 0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.StartTime.gt(new Date(dateFromFilter)));
            }
            if (dateToFilter != 0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.EndTime.lt(new Date(dateToFilter)));
            }
            if (nameContainsFilter != null && !nameContainsFilter.isEmpty()) {
                qb.where(
                        BaseActivitySummaryDao.Properties.Name.like("%" + nameContainsFilter + "%"));
            }
            if (itemsFilter != null) {
                qb.where(
                        BaseActivitySummaryDao.Properties.Id.in(itemsFilter));
            }


            List<BaseActivitySummary> allSummaries = new ArrayList<>();
            // HACK: Populate json in a dummy summary, so stats load faster
            BaseActivitySummary dashboardSummary = new BaseActivitySummary();
            final List<BaseActivitySummary> summaries = qb.build().list();
            dashboardSummary.setSummaryData(StatsContainer.from(
                    device.getDeviceCoordinator().getActivitySummaryParser(device, getContext()),
                    summaries
            ).toJson());
            allSummaries.add(dashboardSummary); // dashboard
            allSummaries.addAll(summaries);
            allSummaries.add(new BaseActivitySummary()); // empty
            setItems(allSummaries, true);
        } catch (Exception e) {
            GB.toast("Error loading activity summaries.", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    @NonNull
    @Override
    public AbstractActivityListingViewHolder<BaseActivitySummary> onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
        switch (viewType) {
            case 0: // dashboard
                return new DashboardViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.activity_summary_dashboard_item, parent, false));
            case 2: // item
                return new ActivityItemViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.activity_list_item, parent, false));
        }

        return super.onCreateViewHolder(parent, viewType);
    }

    public void setActivityKindFilter(int filter) {
        this.activityKindFilter = filter;
    }

    public void setDateFromFilter(long date) {
        this.dateFromFilter = date;
    }

    public void setDateToFilter(long date) {
        this.dateToFilter = date;
    }

    public void setNameContainsFilter(String name) {
        this.nameContainsFilter = name;
    }

    public void setItemsFilter(List<Long> items) {
        this.itemsFilter = items;
    }

    public void setDeviceFilter(long device) {
        this.deviceFilter = device;
    }

    public int gettActivityKindFilter() {
        return this.activityKindFilter;
    }

    public static class ActivityItemViewHolder extends AbstractActivityListingViewHolder<BaseActivitySummary> {
        final View rootView;
        final ActivityListItem activityListItem;

        public ActivityItemViewHolder(@NonNull final View itemView) {
            super(itemView);
            this.rootView = itemView;
            this.activityListItem = new ActivityListItem(itemView);
        }

        @Override
        public void fill(final int position, final BaseActivitySummary summary, final boolean selected) {
            final boolean hasGps;

            if (summary.getGpxTrack() != null) {
                hasGps = true;
            } else if (summary.getSummaryData() != null && summary.getSummaryData().contains(ActivitySummaryEntries.INTERNAL_HAS_GPS)) {
                final ActivitySummaryData summaryData = ActivitySummaryData.fromJson(summary.getSummaryData());
                hasGps = summaryData != null && summaryData.getBoolean(INTERNAL_HAS_GPS, false);
            } else {
                hasGps = false;
            }

            this.activityListItem.update(
                    null,
                    null,
                    ActivityKind.fromCode(summary.getActivityKind()),
                    summary.getName(),
                    -1,
                    -1,
                    -1,
                    -1,
                    summary.getEndTime().getTime() - summary.getStartTime().getTime(),
                    hasGps,
                    summary.getStartTime(),
                    position % 2 == 1,
                    selected
            );
        }
    }

    public class DashboardViewHolder extends AbstractActivityListingViewHolder<BaseActivitySummary> {
        final TextView durationSumView;
        final TextView caloriesBurntSumView;
        final TextView distanceSumView;
        final TextView activeSecondsSumView;
        final TextView timeStartView;
        final TextView timeEndView;
        final TextView activitiesCountView;
        final TextView activityKindView;
        final ImageView activityIconView;
        final ImageView activityIconBigView;

        public DashboardViewHolder(@NonNull final View itemView) {
            super(itemView);

            durationSumView = itemView.findViewById(R.id.summary_dashboard_layout_duration_label);
            caloriesBurntSumView = itemView.findViewById(R.id.summary_dashboard_layout_calories_label);
            distanceSumView = itemView.findViewById(R.id.summary_dashboard_layout_distance_label);
            activeSecondsSumView = itemView.findViewById(R.id.summary_dashboard_layout_active_duration_label);
            timeStartView = itemView.findViewById(R.id.summary_dashboard_layout_from_label);
            timeEndView = itemView.findViewById(R.id.summary_dashboard_layout_to_label);
            activitiesCountView = itemView.findViewById(R.id.summary_dashboard_layout_count_label);
            activityKindView = itemView.findViewById(R.id.summary_dashboard_layout_activity_label);
            activityIconView = itemView.findViewById(R.id.summary_dashboard_layout_activity_icon);
            activityIconBigView = itemView.findViewById(R.id.summary_dashboard_layout_big_activity_icon);
        }

        @Override
        public void fill(final int position, final BaseActivitySummary summary, final boolean selected) {
            int activitiesCount = getItemCount() - 2; // remove dashboard and end spacer

            final DeviceCoordinator coordinator = device.getDeviceCoordinator();

            String summaryData = summary.getSummaryData();
            final StatsContainer stats;
            if (StringUtils.isNotBlank(summaryData)) {
                stats = StatsContainer.fromJson(summaryData);
            } else {
                stats = StatsContainer.from(
                        coordinator.getActivitySummaryParser(device, getContext()),
                        getItems()
                );
            }

            DecimalFormat df = new DecimalFormat("#.##");
            durationSumView.setText(String.format("%s", DateTimeUtils.formatDurationHoursMinutes((long) stats.durationSum, TimeUnit.MILLISECONDS)));
            caloriesBurntSumView.setText(String.format("%s %s", (long) stats.caloriesBurntSum, getContext().getString(R.string.calories_unit)));
            distanceSumView.setText(String.format("%s %s", df.format(stats.distanceSum / 1000), getContext().getString(R.string.km)));
            distanceSumView.setText(FormatUtils.getFormattedDistanceLabel(stats.distanceSum));

            activeSecondsSumView.setText(String.format("%s", DateTimeUtils.formatDurationHoursMinutes((long) stats.activeSecondsSum, TimeUnit.SECONDS)));
            activitiesCountView.setText(String.valueOf(activitiesCount));
            String activityName = getContext().getString(R.string.activity_summaries_all_activities);
            if (gettActivityKindFilter() != 0) {
                ActivityKind activityKind = ActivityKind.fromCode(gettActivityKindFilter());
                activityName = activityKind.getLabel(getContext());
                activityIconView.setImageResource(activityKind.getIcon());
                activityIconBigView.setImageResource(activityKind.getIcon());
            } else if (stats.activityIcon != 0) {
                ActivityKind activityKind = ActivityKind.fromCode(stats.activityIcon);
                activityIconView.setImageResource(activityKind.getIcon());
                activityIconBigView.setImageResource(activityKind.getIcon());
            } else {
                activityIconView.setImageResource(R.drawable.ic_activity_unknown_small);
                activityIconBigView.setImageResource(R.drawable.ic_activity_unknown_small);
            }

            activityKindView.setText(activityName);

            //start and end are inverted when filer not applied, because items are sorted the other way
            timeStartView.setText((dateFromFilter != 0) ? DateTimeUtils.formatDate(new Date(dateFromFilter)) : DateTimeUtils.formatDate(new Date((long) stats.lastItemDate)));
            timeEndView.setText((dateToFilter != 0) ? DateTimeUtils.formatDate(new Date(dateToFilter)) : DateTimeUtils.formatDate(new Date((long) stats.firstItemDate)));
        }
    }

    private static class StatsContainer {
        private static final Gson GSON = new GsonBuilder().create();

        private final double durationSum;
        private final double caloriesBurntSum;
        private final double distanceSum;
        private final double activeSecondsSum;
        private final double firstItemDate;
        private final double lastItemDate;
        private final int activityIcon;

        public StatsContainer(final double durationSum,
                              final double caloriesBurntSum,
                              final double distanceSum,
                              final double activeSecondsSum,
                              final double firstItemDate,
                              final double lastItemDate,
                              final int activityIcon) {
            this.durationSum = durationSum;
            this.caloriesBurntSum = caloriesBurntSum;
            this.distanceSum = distanceSum;
            this.activeSecondsSum = activeSecondsSum;
            this.firstItemDate = firstItemDate;
            this.lastItemDate = lastItemDate;
            this.activityIcon = activityIcon;
        }

        private static StatsContainer from(final ActivitySummaryParser summaryParser,
                                           final List<BaseActivitySummary> activities) {
            double durationSum = 0;
            double caloriesBurntSum = 0;
            double distanceSum = 0;
            double activeSecondsSum = 0;
            double firstItemDate = 0;
            double lastItemDate = 0;
            int activityIcon = 0;
            boolean activitySame = true;

            for (BaseActivitySummary summary : activities) {
                if (summary.getStartTime() == null) continue; //first item is empty, for dashboard

                if (firstItemDate == 0) firstItemDate = summary.getStartTime().getTime();
                lastItemDate = summary.getEndTime().getTime();
                durationSum += summary.getEndTime().getTime() - summary.getStartTime().getTime();

                if (activityIcon == 0) {
                    activityIcon = summary.getActivityKind();
                } else {
                    if (activityIcon != summary.getActivityKind()) {
                        activitySame = false;
                    }
                }

                final ActivitySummaryJsonSummary activitySummaryJsonSummary = new ActivitySummaryJsonSummary(summaryParser, summary);
                ActivitySummaryData summarySubdata = activitySummaryJsonSummary.getSummaryData(false);

                if (summarySubdata != null) {
                    if (summarySubdata.has("caloriesBurnt")) {
                        caloriesBurntSum += summarySubdata.getNumber("caloriesBurnt", 0).doubleValue();
                    }
                    if (summarySubdata.has("distanceMeters")) {
                        distanceSum += summarySubdata.getNumber("distanceMeters", 0).doubleValue();
                    }
                    if (summarySubdata.has("activeSeconds")) {
                        activeSecondsSum += summarySubdata.getNumber("activeSeconds", 0).doubleValue();
                    }
                }
            }

            return new StatsContainer(
                    durationSum,
                    caloriesBurntSum,
                    distanceSum,
                    activeSecondsSum,
                    firstItemDate,
                    lastItemDate,
                    activitySame ? activityIcon : 0
            );
        }

        public static StatsContainer fromJson(final String json) {
            return GSON.fromJson(json, StatsContainer.class);
        }

        public String toJson() {
            return GSON.toJson(this);
        }
    }
}
