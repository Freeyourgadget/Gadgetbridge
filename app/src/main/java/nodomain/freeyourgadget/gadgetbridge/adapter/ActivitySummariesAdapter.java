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
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.dao.query.QueryBuilder;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummaryDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.activities.ActivitySummariesFilter.ALL_DEVICES;

public class ActivitySummariesAdapter extends AbstractActivityListingAdapter<BaseActivitySummary> {
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
            List<BaseActivitySummary> allSummaries = qb.build().list();
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
    protected int getIcon(BaseActivitySummary item) {
        return ActivityKind.getIconId(item.getActivityKind());
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }
}
