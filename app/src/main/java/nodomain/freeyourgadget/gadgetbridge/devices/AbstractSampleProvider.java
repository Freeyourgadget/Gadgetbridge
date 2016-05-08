package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public abstract class AbstractSampleProvider<T extends ActivitySample> implements SampleProvider {
    private static final WhereCondition[] NO_CONDITIONS = new WhereCondition[0];
    private final DaoSession mSession;

    protected AbstractSampleProvider(DaoSession session) {
        mSession = session;
    }

    public DaoSession getSession() {
        return mSession;
    }

    public List<T> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
    }

    public List<T> getActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
    }

    public List<T> getSleepSamples(int timestamp_from, int timestamp_to) {
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

    protected List<T> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        qb.where(MiBandActivitySampleDao.Properties.Timestamp.ge(timestamp_from))
            .where(MiBandActivitySampleDao.Properties.Timestamp.le(timestamp_to), getClauseForActivityType(qb, activityType));
        return qb.build().list();
    }

    private WhereCondition[] getClauseForActivityType(QueryBuilder qb, int activityTypes) {
        if (activityTypes == ActivityKind.TYPE_ALL) {
            return NO_CONDITIONS;
        }

        int[] dbActivityTypes = ActivityKind.mapToDBActivityTypes(activityTypes, this);
        WhereCondition activityTypeCondition = getActivityTypeConditions(qb, dbActivityTypes);
        return new WhereCondition[] { activityTypeCondition };
    }

    private WhereCondition getActivityTypeConditions(QueryBuilder qb, int[] dbActivityTypes) {
        // What a crappy QueryBuilder API ;-( QueryBuilder.or(WhereCondition[]) with a runtime array length
        // check would have worked just fine.
        if (dbActivityTypes.length == 0) {
            return null;
        }
        if (dbActivityTypes.length == 1) {
            return MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[0]);
        }
        if (dbActivityTypes.length == 2) {
            return qb.or(MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[0]),
                MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[1]));
        }
        final int offset = 2;
        int len = dbActivityTypes.length - offset;
        WhereCondition[] trailingConditions = new WhereCondition[len];
        for (int i = 0; i < len; i++) {
            trailingConditions[i] = MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[i + offset]);
        }
        return qb.or(MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[0]),
                MiBandActivitySampleDao.Properties.RawKind.eq(dbActivityTypes[1]),
                trailingConditions);
    }

    public int fetchLatestTimestamp() {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        qb.orderDesc(MiBandActivitySampleDao.Properties.Timestamp);
        qb.limit(1);
        List<T> list = qb.build().list();
        if (list.size() >= 1) {
            return list.get(0).getTimestamp();
        }
        return -1;
    }

    protected abstract AbstractDao<T,?> getSampleDao();
}
