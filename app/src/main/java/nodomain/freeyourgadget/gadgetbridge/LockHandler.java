package nodomain.freeyourgadget.gadgetbridge;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;

/**
 * A dummy DBHandler that does nothing more than implementing the release() method.
 * It is solely used for locking concurrent access to the database session.
 */
public class LockHandler implements DBHandler {

    private final DaoSession session;

    public LockHandler(DaoSession daoSession) {
        session = daoSession;
    }

    @Override
    public void close() {
        GBApplication.releaseDB();
    }

    @Override
    public void closeDb() {

    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return null;
    }

    @Override
    public DaoSession getDaoSession() {
        return session;
    }

    @Override
    public SQLiteDatabase getWritableDatabase() {
        return null;
    }
}
