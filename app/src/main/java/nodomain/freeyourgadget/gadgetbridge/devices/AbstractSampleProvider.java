package nodomain.freeyourgadget.gadgetbridge.devices;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.MiBandActivitySampleDao;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

public abstract class AbstractSampleProvider<T extends AbstractActivitySample> implements SampleProvider<T> {
    private static final WhereCondition[] NO_CONDITIONS = new WhereCondition[0];
    private final DaoSession mSession;

    protected AbstractSampleProvider(DaoSession session) {
        mSession = session;
    }

    public DaoSession getSession() {
        return mSession;
    }

    @Override
    public List<T> getAllActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL);
    }

    @Override
    public List<T> getActivitySamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
    }

    @Override
    public List<T> getSleepSamples(int timestamp_from, int timestamp_to) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP);
    }

    @Override
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

    @Override
    public void addGBActivitySample(T activitySample) {
        getSampleDao().insert(activitySample);
    }

    @Override
    public void addGBActivitySamples(T[] activitySamples) {
        getSampleDao().insertInTx(activitySamples);
    }

//    @Override
//    public void close() {
//        // TESTING: NOOP
//    }
//
//    @Override
//    public SQLiteOpenHelper getHelper() {
//        // TESTING: NOOP
//        return null;
//    }
//
//    @Override
//    public void release() {
//        // TESTING: NOOP
//    }
//
//    @Override
//    public List<ActivitySample> getAllActivitySamples(int tsFrom, int tsTo, SampleProvider provider) {
//        return (List<ActivitySample>) getGBActivitySamples(tsFrom, tsTo, ActivityKind.TYPE_ALL);
//    }
//
//    @Override
//    public List<ActivitySample> getActivitySamples(int tsFrom, int tsTo, SampleProvider provider) {
//        return (List<ActivitySample>) getGBActivitySamples(tsFrom, tsTo, ActivityKind.TYPE_ACTIVITY);
//    }
//
//    @Override
//    public List<ActivitySample> getSleepSamples(int tsFrom, int tsTo, SampleProvider provider) {
//        return (List<ActivitySample>) getGBActivitySamples(tsFrom, tsTo, ActivityKind.TYPE_SLEEP);
//    }
//
//    @Override
//    public SQLiteDatabase getWritableDatabase() {
//        // TESTING: NOOP
//        return null;
//    }
//

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind) {
        List<T> samples = getAllActivitySamples(timestampFrom, timestampTo);
        for (T sample : samples) {
            sample.setRawKind(kind);
        }
        getSampleDao().updateInTx(samples);
    }

    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind) {
        List<T> samples = getGBActivitySamples(timestampFrom, timestampTo, fromKind);
        for (T sample : samples) {
            sample.setRawKind(toKind);
        }
        getSampleDao().updateInTx(samples);
    }

    protected List<T> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        qb.where(MiBandActivitySampleDao.Properties.Timestamp.ge(timestamp_from))
            .where(MiBandActivitySampleDao.Properties.Timestamp.le(timestamp_to), getClauseForActivityType(qb, activityType));
        List<T> samples = qb.build().list();
        for (T sample : samples) {
            sample.setProvider(this);
        }
        return samples;
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

    protected abstract AbstractDao<T,?> getSampleDao();
}
