package nodomain.freeyourgadget.gadgetbridge.devices;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;

/**
 * Base class for all sample providers. A Sample provider is device specific and provides
 * access to the device specific samples. There are both read and write operations.
 * @param <T> the sample type
 */
public abstract class AbstractSampleProvider<T extends AbstractActivitySample> implements SampleProvider<T> {
    private static final WhereCondition[] NO_CONDITIONS = new WhereCondition[0];
    private final DaoSession mSession;
    private final GBDevice mDevice;

    protected AbstractSampleProvider(GBDevice device, DaoSession session) {
        mDevice = device;
        mSession = session;
    }

    public GBDevice getDevice() {
        return mDevice;
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
        if (getRawKindSampleProperty() != null) {
            return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
        } else {
            return getActivitySamplesByActivityFilter(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY);
        }
    }

    @Override
    public List<T> getSleepSamples(int timestamp_from, int timestamp_to) {
        if (getRawKindSampleProperty() != null) {
            return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP);
        } else {
            return getActivitySamplesByActivityFilter(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP);
        }
    }

    @Override
    public void addGBActivitySample(T activitySample) {
        getSampleDao().insertOrReplace(activitySample);
    }

    @Override
    public void addGBActivitySamples(T[] activitySamples) {
        getSampleDao().insertOrReplaceInTx(activitySamples);
    }

    @Nullable
    @Override
    public T getLatestActivitySample() {
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no sample
            return null;
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId())).orderDesc(getTimestampSampleProperty()).limit(1);
        List<T> samples = qb.build().list();
        if (samples.isEmpty()) {
            return null;
        }
        return samples.get(0);
    }

    protected List<T> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityType) {
        if (getRawKindSampleProperty() == null && activityType != ActivityKind.TYPE_ALL) {
            // if we do not have a raw kind property we cannot query anything else then TYPE_ALL
            return Collections.emptyList();
        }
        QueryBuilder<T> qb = getSampleDao().queryBuilder();
        Property timestampProperty = getTimestampSampleProperty();
        Device dbDevice = DBHelper.findDevice(getDevice(), getSession());
        if (dbDevice == null) {
            // no device, no samples
            return Collections.emptyList();
        }
        Property deviceProperty = getDeviceIdentifierSampleProperty();
        qb.where(deviceProperty.eq(dbDevice.getId()), timestampProperty.ge(timestamp_from))
            .where(timestampProperty.le(timestamp_to), getClauseForActivityType(qb, activityType));
        List<T> samples = qb.build().list();
        for (T sample : samples) {
            sample.setProvider(this);
        }
        detachFromSession();
        return samples;
    }

    /**
     * Detaches all samples of this type from the session. Changes to them may not be
     * written back to the database.
     *
     * Subclasses should call this method after performing custom queries.
     */
    protected void detachFromSession() {
        getSampleDao().detachAll();
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
        Property rawKindProperty = getRawKindSampleProperty();
        if (rawKindProperty == null) {
            return null;
        }

        if (dbActivityTypes.length == 1) {
            return rawKindProperty.eq(dbActivityTypes[0]);
        }
        if (dbActivityTypes.length == 2) {
            return qb.or(rawKindProperty.eq(dbActivityTypes[0]),
                    rawKindProperty.eq(dbActivityTypes[1]));
        }
        final int offset = 2;
        int len = dbActivityTypes.length - offset;
        WhereCondition[] trailingConditions = new WhereCondition[len];
        for (int i = 0; i < len; i++) {
            trailingConditions[i] = rawKindProperty.eq(dbActivityTypes[i + offset]);
        }
        return qb.or(rawKindProperty.eq(dbActivityTypes[0]),
                rawKindProperty.eq(dbActivityTypes[1]),
                trailingConditions);
    }

    private List<T> getActivitySamplesByActivityFilter(int timestamp_from, int timestamp_to, int activityFilter) {
        List<T> samples = getAllActivitySamples(timestamp_from, timestamp_to);
        List<T> filteredSamples = new ArrayList<>();

        for (T sample : samples) {
            if ((sample.getKind() & activityFilter) != 0) {
                filteredSamples.add(sample);
            }
        }
        return filteredSamples;
    }

    public abstract AbstractDao<T,?> getSampleDao();

    @Nullable
    protected abstract Property getRawKindSampleProperty();

    @NonNull
    protected abstract Property getTimestampSampleProperty();

    @NonNull
    protected abstract Property getDeviceIdentifierSampleProperty();
}
