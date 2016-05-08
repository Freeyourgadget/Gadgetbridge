package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

import de.greenrobot.dao.AbstractDao;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

public class DaoHandler implements DBHandler {

    private final DaoMaster mDaoMaster;
    private final SQLiteOpenHelper openHelper;

    public DaoHandler(DaoMaster master, DaoMaster.DevOpenHelper helper) {
        mDaoMaster = master;
        openHelper = helper;
    }

    @Override
    public void close() {
        getHelper().close();
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return openHelper;
    }

    @Override
    public void release() {
        GBApplication.releaseDB();
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
    public void addGBActivitySample(int timestamp, int provider, int intensity, int steps, int kind, int heartrate) {

    }

    @Override
    public void addGBActivitySamples(ActivitySample[] activitySamples, AbstractDao<ActivitySample,?> dao) {
        for (ActivitySample sample : activitySamples) {
            dao.insert(sample);
        }
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return mDaoMaster.getDatabase();
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
