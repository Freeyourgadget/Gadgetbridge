/*  Copyright (C) 2017-2020 Carsten Pfeiffer, Daniele Gobbetti

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
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
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
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySummaryJsonSummary;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesFilter.ALL_DEVICES;

public class ActivitySummariesAdapter extends AbstractActivityListingAdapter<BaseActivitySummary> {
    protected static final Logger LOG = LoggerFactory.getLogger(ActivitySummariesAdapter.class);
    private final GBDevice device;
    long dateFromFilter = 0;
    long dateToFilter = 0;
    long deviceFilter;
    String nameContainsFilter;
    List<Long> itemsFilter;
    private int activityKindFilter;
    private int backgroundColor = 0;

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
            if (nameContainsFilter != null && nameContainsFilter.length() > 0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.Name.like("%" + nameContainsFilter + "%"));
            }
            if (itemsFilter != null) {
                qb.where(
                        BaseActivitySummaryDao.Properties.Id.in(itemsFilter));
            }


            List<BaseActivitySummary> allSummaries = new ArrayList<>();
            allSummaries.add(new BaseActivitySummary());
            allSummaries.addAll(qb.build().list());
            setItems(allSummaries, true);
        } catch (Exception e) {
            GB.toast("Error loading activity summaries.", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
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

    public void setItemsFilter(List items) {
        this.itemsFilter = items;
    }

    public void setDeviceFilter(long device) {
        this.deviceFilter = device;
    }

    public int gettActivityKindFilter() {
        return this.activityKindFilter;
    }

    @Override
    protected View fill_dashboard(BaseActivitySummary item, int position, View view, ViewGroup parent, Context context) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.activity_summary_dashboard_item, parent, false);

        double durationSum = 0;
        double caloriesBurntSum = 0;
        double distanceSum = 0;
        double activeSecondsSum = 0;
        double firstItemDate = 0;
        double lastItemDate = 0;
        int activitiesCount = getCount() - 1;
        int activityIcon = 0;
        boolean activitySame = true;

        TextView durationSumView = view.findViewById(R.id.summary_dashboard_layout_duration_label);
        TextView caloriesBurntSumView = view.findViewById(R.id.summary_dashboard_layout_calories_label);
        TextView distanceSumView = view.findViewById(R.id.summary_dashboard_layout_distance_label);
        TextView activeSecondsSumView = view.findViewById(R.id.summary_dashboard_layout_active_duration_label);
        TextView timeStartView = view.findViewById(R.id.summary_dashboard_layout_from_label);
        TextView timeEndView = view.findViewById(R.id.summary_dashboard_layout_to_label);
        TextView activitiesCountView = view.findViewById(R.id.summary_dashboard_layout_count_label);
        TextView activityKindView = view.findViewById(R.id.summary_dashboard_layout_activity_label);
        ImageView activityIconView = view.findViewById(R.id.summary_dashboard_layout_activity_icon);
        ImageView activityIconBigView = view.findViewById(R.id.summary_dashboard_layout_big_activity_icon);

        for (BaseActivitySummary sportitem : getItems()) {
            if (sportitem.getStartTime() == null) continue; //first item is empty, for dashboard

            if (firstItemDate == 0) firstItemDate = sportitem.getStartTime().getTime();
            lastItemDate = sportitem.getEndTime().getTime();
            durationSum += sportitem.getEndTime().getTime() - sportitem.getStartTime().getTime();

            if (activityIcon == 0) {
                activityIcon = sportitem.getActivityKind();
            } else {
                if (activityIcon != sportitem.getActivityKind()) {
                    activitySame = false;
                }
            }


            ActivitySummaryJsonSummary activitySummaryJsonSummary = new ActivitySummaryJsonSummary(sportitem);
            JSONObject summarySubdata = activitySummaryJsonSummary.getSummaryData();

            if (summarySubdata != null) {
                try {
                    if (summarySubdata.has("caloriesBurnt")) {
                        caloriesBurntSum += summarySubdata.getJSONObject("caloriesBurnt").getDouble("value");
                    }
                    if (summarySubdata.has("distanceMeters")) {
                        distanceSum += summarySubdata.getJSONObject("distanceMeters").getDouble("value");
                    }
                    if (summarySubdata.has("activeSeconds")) {
                        activeSecondsSum += summarySubdata.getJSONObject("activeSeconds").getDouble("value");
                    }
                } catch (JSONException e) {
                    LOG.error("SportsActivity", e);
                }
            }
        }
        DecimalFormat df = new DecimalFormat("#.##");
        durationSumView.setText(String.format("%s", DateTimeUtils.formatDurationHoursMinutes((long) durationSum, TimeUnit.MILLISECONDS)));
        caloriesBurntSumView.setText(String.format("%s %s", (long) caloriesBurntSum, context.getString(R.string.calories_unit)));
        distanceSumView.setText(String.format("%s %s", df.format(distanceSum / 1000), context.getString(R.string.km)));
        distanceSumView.setText(getLabel(distanceSum));


        activeSecondsSumView.setText(String.format("%s", DateTimeUtils.formatDurationHoursMinutes((long) activeSecondsSum, TimeUnit.SECONDS)));
        activitiesCountView.setText(String.valueOf(activitiesCount));
        String activityName = context.getString(R.string.activity_summaries_all_activities);
        if (gettActivityKindFilter() != 0) {
            activityName = ActivityKind.asString(gettActivityKindFilter(), context);
            activityIconView.setImageResource(ActivityKind.getIconId(gettActivityKindFilter()));
            activityIconBigView.setImageResource(ActivityKind.getIconId(gettActivityKindFilter()));
        } else {
            if (activitySame) {
                activityIconView.setImageResource(ActivityKind.getIconId(activityIcon));
                activityIconBigView.setImageResource(ActivityKind.getIconId(activityIcon));
            }
        }

        activityKindView.setText(activityName);

        //start and end are inverted when filer not applied, because items are sorted the other way
        timeStartView.setText((dateFromFilter != 0) ? DateTimeUtils.formatDate(new Date(dateFromFilter)) : DateTimeUtils.formatDate(new Date((long) lastItemDate)));
        timeEndView.setText((dateToFilter != 0) ? DateTimeUtils.formatDate(new Date(dateToFilter)) : DateTimeUtils.formatDate(new Date((long) firstItemDate)));
        return view;
    }

    @Override
    protected String getDateLabel(BaseActivitySummary item) {
        Date startTime = item.getStartTime();
        String separator = ",";
        if (startTime != null) {
            String activityDay;

            if (DateUtils.isToday(startTime.getTime())) {
                activityDay = getContext().getString(R.string.activity_summary_today);
            } else if (DateTimeUtils.isYesterday(startTime)) {
                activityDay = getContext().getString(R.string.activity_summary_yesterday);
            } else {
                activityDay = DateTimeUtils.formatDate(startTime);
            }
            String activityTime = DateTimeUtils.formatTime(startTime.getHours(), startTime.getMinutes());
            return String.format("%s%s %s", activityDay, separator, activityTime);
        }
        return "Unknown time";
    }

    @Override
    protected boolean hasGPS(BaseActivitySummary item) {
        if (item.getGpxTrack() != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected boolean hasDate(BaseActivitySummary item) {
        return true;
    }

    @Override
    protected String getTimeFrom(BaseActivitySummary item) {
        Date time = item.getStartTime();
        return DateTimeUtils.formatTime(time.getHours(), time.getMinutes());
    }

    @Override
    protected String getTimeTo(BaseActivitySummary item) {
        Date time = item.getEndTime();
        return DateTimeUtils.formatTime(time.getHours(), time.getMinutes());
    }

    @Override
    protected String getActivityName(BaseActivitySummary item) {
        String activityLabel = item.getName();
        String separator = ",";
        if (activityLabel == null) {
            activityLabel = "";
            separator = "";
        }

        String activityKindName = ActivityKind.asString(item.getActivityKind(), getContext());
        return String.format("%s%s %s", activityKindName, separator, activityLabel);
    }

    @Override
    protected String getStepLabel(BaseActivitySummary item) {
        return null;
    }

    @Override
    protected String getDistanceLabel(BaseActivitySummary item) {
        return null;
    }

    protected String getLabel(double distance) {
        double distanceMetric = distance;
        double distanceImperial = distanceMetric * 3.28084f;
        double distanceFormatted = 0;

        String unit = "###m";
        distanceFormatted = distanceMetric;
        if (distanceMetric > 2000) {
            distanceFormatted = distanceMetric / 1000;
            unit = "###.#km";
        }

        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            unit = "###ft";
            distanceFormatted = distanceImperial;
            if (distanceImperial > 6000) {
                distanceFormatted = distanceImperial * 0.0001893939f;
                unit = "###.#mi";
            }
        }
        DecimalFormat df = new DecimalFormat(unit);
        return df.format(distanceFormatted);
    }

    @Override
    protected String getHrLabel(BaseActivitySummary item) {
        return null;
    }

    @Override
    protected String getIntensityLabel(BaseActivitySummary item) {
        return null;
    }

    @Override
    protected String getDurationLabel(BaseActivitySummary item) {
        Long duration = item.getEndTime().getTime() - item.getStartTime().getTime();
        return DateTimeUtils.formatDurationHoursMinutes(duration, TimeUnit.MILLISECONDS);
    }

    @Override
    protected String getSpeedLabel(BaseActivitySummary item) {
        return null;
    }

    @Override
    protected String getSessionCountLabel(BaseActivitySummary item) {
        return "";
    }

    @Override
    protected boolean hasHR(BaseActivitySummary item) {
        return false;
    }

    @Override
    protected boolean hasIntensity(BaseActivitySummary item) {
        return false;
    }

    @Override
    protected boolean hasDistance(BaseActivitySummary item) {
        return false;
    }

    @Override
    protected boolean hasSteps(BaseActivitySummary item) {
        return false;
    }

    @Override
    protected boolean hasTotalSteps(BaseActivitySummary item) {
        return false;
    }

    @Override
    protected boolean isSummary(BaseActivitySummary item, int position) {
        return position == 0;
    }

    @Override
    protected boolean isEmptySession(BaseActivitySummary item, int position) { return false; }

    @Override
    protected boolean isEmptySummary(BaseActivitySummary item) {
        return false;
    }

    @Override
    protected String getStepTotalLabel(BaseActivitySummary item) {
        return null;
    }

    @Override
    protected int getIcon(BaseActivitySummary item) {
        return ActivityKind.getIconId(item.getActivityKind());
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
