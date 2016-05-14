package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

/**
 * A dummy DBHandler that does nothing more than implementing the release() method.
 * It is solely used for locking concurrent access to the database session.
 */
public class LockHandler implements DBHandler {

    @Override
    public void release() {

    }

    @Override
    public void close() {

    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return null;
    }

    @Override
    public List<ActivitySample> getAllActivitySamples(int tsFrom, int tsTo, SampleProvider provider) {
        return null;
    }

    @Override
    public List<ActivitySample> getActivitySamples(int tsFrom, int tsTo, SampleProvider provider) {
        return null;
    }

    @Override
    public List<ActivitySample> getSleepSamples(int tsFrom, int tsTo, SampleProvider provider) {
        return null;
    }

    @Override
    public void addGBActivitySample(AbstractActivitySample sample) {

    }

    @Override
    public void addGBActivitySamples(AbstractActivitySample[] activitySamples) {

    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return null;
    }

    @Override
    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind, SampleProvider provider) {

    }

    @Override
    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind, SampleProvider provider) {

    }

    @Override
    public int fetchLatestTimestamp(SampleProvider provider) {
        return 0;
    }
}
