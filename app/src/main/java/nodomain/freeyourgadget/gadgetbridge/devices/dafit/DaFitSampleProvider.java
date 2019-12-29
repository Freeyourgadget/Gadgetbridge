/*  Copyright (C) 2019 krzys_h

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
package nodomain.freeyourgadget.gadgetbridge.devices.dafit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.SqlUtils;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DaFitActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaFitActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class DaFitSampleProvider extends AbstractSampleProvider<DaFitActivitySample> {
    public static final int SOURCE_NOT_MEASURED = -1;
    public static final int SOURCE_STEPS_REALTIME = 1;     // steps gathered at realtime from the steps characteristic
    public static final int SOURCE_STEPS_SUMMARY = 2;      // steps gathered from the daily summary
    public static final int SOURCE_STEPS_IDLE = 3;         // idle sample inserted because the user was not moving (to differentiate from missing data because watch not connected)
    public static final int SOURCE_SLEEP_SUMMARY = 4;      // data collected from the sleep function
    public static final int SOURCE_SINGLE_MEASURE = 5;     // heart rate / blood data gathered from the "single measurement" function
    public static final int SOURCE_TRAINING_HEARTRATE = 6; // heart rate data collected from the training function
    public static final int SOURCE_BATTERY = 7;            // battery report

    public static final int ACTIVITY_NOT_MEASURED = -1;
    public static final int ACTIVITY_TRAINING_WALK = DaFitConstants.TRAINING_TYPE_WALK;
    public static final int ACTIVITY_TRAINING_RUN = DaFitConstants.TRAINING_TYPE_RUN;
    public static final int ACTIVITY_TRAINING_BIKING = DaFitConstants.TRAINING_TYPE_BIKING;
    public static final int ACTIVITY_TRAINING_ROPE = DaFitConstants.TRAINING_TYPE_ROPE;
    public static final int ACTIVITY_TRAINING_BADMINTON = DaFitConstants.TRAINING_TYPE_BADMINTON;
    public static final int ACTIVITY_TRAINING_BASKETBALL = DaFitConstants.TRAINING_TYPE_BASKETBALL;
    public static final int ACTIVITY_TRAINING_FOOTBALL = DaFitConstants.TRAINING_TYPE_FOOTBALL;
    public static final int ACTIVITY_TRAINING_SWIM = DaFitConstants.TRAINING_TYPE_SWIM;
    public static final int ACTIVITY_TRAINING_MOUNTAINEERING = DaFitConstants.TRAINING_TYPE_MOUNTAINEERING;
    public static final int ACTIVITY_TRAINING_TENNIS = DaFitConstants.TRAINING_TYPE_TENNIS;
    public static final int ACTIVITY_TRAINING_RUGBY = DaFitConstants.TRAINING_TYPE_RUGBY;
    public static final int ACTIVITY_TRAINING_GOLF = DaFitConstants.TRAINING_TYPE_GOLF;
    public static final int ACTIVITY_SLEEP_LIGHT = 16;
    public static final int ACTIVITY_SLEEP_RESTFUL = 17;
    public static final int ACTIVITY_SLEEP_START = 18;
    public static final int ACTIVITY_SLEEP_END = 19;

    public DaFitSampleProvider(GBDevice device, DaoSession session) {
        super(device, session);
    }

    @Override
    public AbstractDao<DaFitActivitySample, ?> getSampleDao() {
        return getSession().getDaFitActivitySampleDao();
    }

    @NonNull
    @Override
    protected Property getTimestampSampleProperty() {
        return DaFitActivitySampleDao.Properties.Timestamp;
    }

    @Nullable
    @Override
    protected Property getRawKindSampleProperty() {
        return DaFitActivitySampleDao.Properties.RawKind;
    }

    @NonNull
    @Override
    protected Property getDeviceIdentifierSampleProperty() {
        return DaFitActivitySampleDao.Properties.DeviceId;
    }

    @Override
    public DaFitActivitySample createActivitySample() {
        return new DaFitActivitySample();
    }

    @Override
    public int normalizeType(int rawType) {
        if (rawType == ACTIVITY_NOT_MEASURED)
            return ActivityKind.TYPE_NOT_MEASURED;
        else if (rawType == ACTIVITY_SLEEP_LIGHT)
            return ActivityKind.TYPE_LIGHT_SLEEP;
        else if (rawType == ACTIVITY_SLEEP_RESTFUL)
            return ActivityKind.TYPE_DEEP_SLEEP;
        else if (rawType == ACTIVITY_SLEEP_START || rawType == ACTIVITY_SLEEP_END)
            return ActivityKind.TYPE_NOT_MEASURED;
        else if (rawType == ACTIVITY_TRAINING_WALK)
            return ActivityKind.TYPE_WALKING;
        else if (rawType == ACTIVITY_TRAINING_RUN)
            return ActivityKind.TYPE_RUNNING;
        else if (rawType == ACTIVITY_TRAINING_BIKING)
            return ActivityKind.TYPE_CYCLING;
        else if (rawType == ACTIVITY_TRAINING_SWIM)
            return ActivityKind.TYPE_SWIMMING;
        else if (rawType == ACTIVITY_TRAINING_ROPE || rawType == ACTIVITY_TRAINING_BADMINTON ||
            rawType == ACTIVITY_TRAINING_BASKETBALL || rawType == ACTIVITY_TRAINING_FOOTBALL ||
            rawType == ACTIVITY_TRAINING_MOUNTAINEERING || rawType == ACTIVITY_TRAINING_TENNIS ||
            rawType == ACTIVITY_TRAINING_RUGBY || rawType == ACTIVITY_TRAINING_GOLF)
            return ActivityKind.TYPE_EXERCISE;
        else
            return ActivityKind.TYPE_ACTIVITY;
    }

    @Override
    public int toRawActivityKind(int activityKind) {
        if (activityKind == ActivityKind.TYPE_NOT_MEASURED)
            return ACTIVITY_NOT_MEASURED;
        else if (activityKind == ActivityKind.TYPE_LIGHT_SLEEP)
            return ACTIVITY_SLEEP_LIGHT;
        else if (activityKind == ActivityKind.TYPE_DEEP_SLEEP)
            return ACTIVITY_SLEEP_RESTFUL;
        else if (activityKind == ActivityKind.TYPE_ACTIVITY)
            return ACTIVITY_NOT_MEASURED; // TODO: ?
        else
            throw new IllegalArgumentException("Invalid Gadgetbridge activity kind: " + activityKind);
    }

    @Override
    public float normalizeIntensity(int rawIntensity) {
        if (rawIntensity == ActivitySample.NOT_MEASURED)
            return Float.NEGATIVE_INFINITY;
        else
            return rawIntensity;
    }

    /**
     * Set the activity kind from NOT_MEASURED to new_raw_activity_kind on the given range
     * @param timestamp_from the start timestamp
     * @param timestamp_to the end timestamp
     * @param new_raw_activity_kind the activity kind to set
     */
    public void updateActivityInRange(int timestamp_from, int timestamp_to, int new_raw_activity_kind)
    {
        // greenDAO does not provide a bulk update functionality, and manual update fails because
        // of no primary key

        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null)
            throw new IllegalStateException();
        Property deviceProperty = getDeviceIdentifierSampleProperty();

        /*QueryBuilder<DaFitActivitySample> qb = getSampleDao().queryBuilder();
        qb.where(deviceProperty.eq(dbDevice.getId()))
            .where(timestampProperty.ge(timestamp_from), timestampProperty.le(timestamp_to))
            .where(getRawKindSampleProperty().eq(ACTIVITY_NOT_MEASURED));
        List<DaFitActivitySample> samples = qb.build().list();
        for (DaFitActivitySample sample : samples) {
            sample.setProvider(this);
            sample.setRawKind(new_raw_activity_kind);
            sample.update();
        }*/

        String tablename = getSampleDao().getTablename();
        String baseSql = SqlUtils.createSqlUpdate(tablename, new String[] { getRawKindSampleProperty().columnName }, new String[] { });
        StringBuilder builder = new StringBuilder(baseSql);

        List<Object> values = new ArrayList<>();
        values.add(new_raw_activity_kind);
        List<WhereCondition> whereConditions = new ArrayList<>();
        whereConditions.add(deviceProperty.eq(dbDevice.getId()));
        whereConditions.add(timestampProperty.ge(timestamp_from));
        whereConditions.add(timestampProperty.le(timestamp_to));
        whereConditions.add(getRawKindSampleProperty().eq(ACTIVITY_NOT_MEASURED));

        ListIterator<WhereCondition> iter = whereConditions.listIterator();
        while (iter.hasNext()) {
            if (iter.hasPrevious()) {
                builder.append(" AND ");
            }
            WhereCondition condition = iter.next();
            condition.appendTo(builder, tablename);
            condition.appendValuesTo(values);
        }
        getSampleDao().getDatabase().execSQL(builder.toString(), values.toArray());
    }
}