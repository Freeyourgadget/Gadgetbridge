package nodomain.freeyourgadget.gadgetbridge.devices;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_PROVIDER;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TIMESTAMP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;

public abstract class AbstractSampleProvider implements SampleProvider {
    public List<ActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
    }

    public List<ActivitySample> getActivitySamples(int tsFrom, int tsTo) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
    }

    public List<ActivitySample> getSleepSamples(int tsFrom, int tsTo) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP);
    }

    public void addGBActivitySample(int timestamp, int provider, int intensity, int steps, int kind, int heartrate) {

    }

    public void addGBActivitySamples(ActivitySample[] activitySamples) {

    }

//    SQLiteDatabase getWritableDatabase();

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind) {

    }

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind) {

    }

    protected List<ActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        QueryBuilder qb = getSampleDao().queryBuilder();
                qb.where(MiBandActivitySampleDao.Properties.Timestamp.ge(timestamp_from);
                qb.and(MiBandActivitySampleDao.Properties.Timestamp.le(timestamp_to);
                addClauseForActivityType(qb, activityType);
    }

    private void addClauseForActivityType(QueryBuilder qb, int activityTypes) {
        if (activityTypes == ActivityKind.TYPE_ALL) {
            return; // no further restriction
        }

        int[] dbActivityTypes = ActivityKind.mapToDBActivityTypes(activityTypes, this);
        WhereCondition[] activityTypeConditions = getActivityTypeConditions(dbActivityTypes);
        qb.and(qb.or(activityTypeConditions));
    }

    private WhereCondition[] getActivityTypeConditions(int[] dbActivityTypes) {
        WhereCondition[] result = new WhereCondition[dbActivityTypes.length];
        for (int i = 0; i < dbActivityTypes.length; i++) {
            result[i] = MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[i]);
        }
        return result;
    }

    public int fetchLatestTimestamp() {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, new String[]{KEY_TIMESTAMP}, KEY_PROVIDER + "=" + String.valueOf(provider.getID()), null, null, null, KEY_TIMESTAMP + " DESC", "1")) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
        }
        return -1;
    }

    protected abstract AbstractDao<? extends ActivitySample,?> getSampleDao();
}
