package nodomain.freeyourgadget.gadgetbridge;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBOpenHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

/**
 * A dummy DBHandler that does nothing more than implementing the release() method.
 * It is solely used for locking concurrent access to the database session.
 */
public class LockHandler implements DBHandler {

    private final DaoMaster daoMaster;
    private DaoSession session;
    private final SQLiteOpenHelper helper;

    public LockHandler(DaoMaster daoMaster, DBOpenHelper helper) {
        this.daoMaster = daoMaster;
        this.helper = helper;
        session = daoMaster.newSession();
    }

    @Override
    public void close() {
        GBApplication.releaseDB();
    }

    @Override
    public synchronized void openDb() {
        if (session != null) {
            throw new IllegalStateException("session must be null");
        }
        // this will create completely new db instances. This handler will be dead
        GBApplication.setupDatabase(GBApplication.getContext());
    }

    @Override
    public synchronized void closeDb() {
        if (session == null) {
            throw new IllegalStateException("session must not be null");
        }
        session.clear();
        session.getDatabase().close();
        session = null;
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return helper;
    }

    @Override
    public DaoSession getDaoSession() {
        return session;
    }

    @Override
    public SQLiteDatabase getDatabase() {
        return daoMaster.getDatabase();
    }
}
