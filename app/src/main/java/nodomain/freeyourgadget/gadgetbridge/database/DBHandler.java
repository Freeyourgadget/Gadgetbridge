package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public interface DBHandler extends AutoCloseable {
    /**
     * Closes the database.
     */
    void closeDb();

    SQLiteOpenHelper getHelper();

    /**
     * Releases the DB handler. No access may be performed after calling this method.
     * Same as calling {@link GBApplication#releaseDB()}
     */
    void close() throws Exception;

    List<ActivitySample> getAllActivitySamples(int tsFrom, int tsTo, SampleProvider provider);

    List<ActivitySample> getActivitySamples(int tsFrom, int tsTo, SampleProvider provider);

    List<ActivitySample> getSleepSamples(int tsFrom, int tsTo, SampleProvider provider);

    void addGBActivitySample(AbstractActivitySample sample);

    void addGBActivitySamples(AbstractActivitySample[] activitySamples);

    SQLiteDatabase getWritableDatabase();

    void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind, SampleProvider provider);

    void changeStoredSamplesType(int timestampFrom, int timestampTo, int fromKind, int toKind, SampleProvider provider);

    int fetchLatestTimestamp(SampleProvider provider);

    DaoSession getDaoSession();
}
