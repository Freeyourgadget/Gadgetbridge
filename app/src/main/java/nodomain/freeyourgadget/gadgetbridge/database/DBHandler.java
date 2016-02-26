package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public interface DBHandler {
    SQLiteOpenHelper getHelper();

    /**
     * Releases the DB handler. No access may be performed after calling this method.
     * Same as calling {@link GBApplication#releaseDB()}
     */
    void release();

    List<ActivitySample> getAllActivitySamples(int tsFrom, int tsTo, SampleProvider provider);

    List<ActivitySample> getActivitySamples(int tsFrom, int tsTo, SampleProvider provider);

    List<ActivitySample> getSleepSamples(int tsFrom, int tsTo, SampleProvider provider);

    void addGBActivitySample(int timestamp, byte provider, short intensity, short steps, byte kind, short heartrate);

    void addGBActivitySamples(ActivitySample[] activitySamples);

    SQLiteDatabase getWritableDatabase();

    void changeStoredSamplesType(int timestampFrom, int timestampTo, byte kind, SampleProvider provider);

    int fetchLatestTimestamp(SampleProvider provider);

}
