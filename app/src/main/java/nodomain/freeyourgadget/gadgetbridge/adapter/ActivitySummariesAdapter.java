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

public class ActivitySummariesAdapter extends AbstractItemAdapter<BaseActivitySummary> {
    private final GBDevice device;
    private int activityKindFilter;
    long dateFromFilter=0;
    long dateToFilter=0;
    String nameContainsFilter;

    public ActivitySummariesAdapter(Context context, GBDevice device, int activityKindFilter, long dateFromFilter, long dateToFilter, String nameContainsFilter) {
        super(context);
        this.device = device;
        this.activityKindFilter = activityKindFilter;
        this.dateFromFilter=dateFromFilter;
        this.dateToFilter=dateToFilter;
        this.nameContainsFilter=nameContainsFilter;
        loadItems();
    }

    @Override
    public void loadItems() {
        try (DBHandler handler = GBApplication.acquireDB()) {
            BaseActivitySummaryDao summaryDao = handler.getDaoSession().getBaseActivitySummaryDao();
            Device dbDevice = DBHelper.findDevice(device, handler.getDaoSession());

            QueryBuilder<BaseActivitySummary> qb = summaryDao.queryBuilder();
            qb.where(
                    BaseActivitySummaryDao.Properties.DeviceId.eq(
                            dbDevice.getId())).orderDesc(BaseActivitySummaryDao.Properties.StartTime);

            if (activityKindFilter !=0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.ActivityKind.eq(activityKindFilter));
            }

            if (dateFromFilter !=0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.StartTime.gt(new Date(dateFromFilter)));
            }
            if (dateToFilter !=0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.EndTime.lt(new Date(dateToFilter)));
            }
            if (nameContainsFilter !=null && nameContainsFilter.length() > 0) {
                qb.where(
                        BaseActivitySummaryDao.Properties.Name.like("%" + nameContainsFilter + "%"));
            }

            List<BaseActivitySummary> allSummaries = qb.build().list();
            setItems(allSummaries, true);
        } catch (Exception e) {
            GB.toast("Error loading activity summaries.", Toast.LENGTH_SHORT, GB.ERROR, e);
        }
    }

    public void setActivityKindFilter(int filter){
        this.activityKindFilter=filter;
    }
    public void setDateFromFilter(long date){
        this.dateFromFilter=date;
    }
    public void setDateToFilter(long date){
        this.dateToFilter=date;
    }
    public void setNameContainsFilter(String name){
        this.nameContainsFilter=name;
    }


    @Override
    protected String getName(BaseActivitySummary item) {
        String name = item.getName();
        if (name == null) name="";
        String gpxTrack = item.getGpxTrack();
        String hasGps = " ";
        if (gpxTrack != null) {
            hasGps=" 🛰️ ";
        }
        return ActivityKind.asString(item.getActivityKind(), getContext())+ hasGps + name;
    }

    @Override
    protected String getDetails(BaseActivitySummary item) {
        Date startTime = item.getStartTime();

        if (startTime != null) {
            String activityDay;
            String activityTime;
            String activityDayTime;
            Long duration = item.getEndTime().getTime() - item.getStartTime().getTime();

            if (DateUtils.isToday(startTime.getTime())) {
                activityDay = getContext().getString(R.string.activity_summary_today);
            } else if (DateTimeUtils.isYesterday(startTime)) {
                activityDay = getContext().getString(R.string.activity_summary_yesterday);
            } else {
                activityDay = DateTimeUtils.formatDate(startTime);
            }
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startTime);
            int hours = calendar.get(Calendar.HOUR_OF_DAY);
            int minutes = calendar.get(Calendar.MINUTE);

            activityTime = DateTimeUtils.formatTime(hours, minutes);
            activityDayTime = String.format("%s, %s", activityDay, activityTime);

            return activityDayTime + " (" + DateTimeUtils.formatDurationHoursMinutes(duration, TimeUnit.MILLISECONDS) + ")";
        }
        return "Unknown time";
    }

    @Override
    protected int getIcon(BaseActivitySummary item) {
        return ActivityKind.getIconId(item.getActivityKind());
    }
}
