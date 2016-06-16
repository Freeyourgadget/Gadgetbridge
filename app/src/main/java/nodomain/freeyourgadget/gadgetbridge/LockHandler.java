package nodomain.freeyourgadget.gadgetbridge;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBOpenHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

/**
 * Provides lowlevel access to the database.
 */
public class LockHandler implements DBHandler {

    private DaoMaster daoMaster = null;
    private DaoSession session = null;
    private SQLiteOpenHelper helper = null;

    public LockHandler() {
    }

    public void init(DaoMaster daoMaster, DBOpenHelper helper) {
        if (isValid()) {
            throw new IllegalStateException("DB must be closed before initializing it again");
        }
        if (daoMaster == null) {
            throw new IllegalArgumentException("daoMaster must not be null");
        }
        if (helper == null) {
            throw new IllegalArgumentException("helper must not be null");
        }
        this.daoMaster = daoMaster;
        this.helper = helper;

        session = daoMaster.newSession();
        if (session == null) {
            throw new RuntimeException("Unable to create database session");
        }
        if (helper.importOldDbIfNecessary(daoMaster, this)) {
            session.clear();
            session = daoMaster.newSession();
        }

    }

    @Override
    public DaoMaster getDaoMaster() {
        return daoMaster;
    }

    private boolean isValid() {
        return daoMaster != null;
    }

    private void ensureValid() {
        if (!isValid()) {
            throw new IllegalStateException("LockHandler is not in a valid state");
        }
    }

    @Override
    public void close() {
        ensureValid();
        GBApplication.releaseDB();
    }

    @Override
    public synchronized void openDb() {
        if (session != null) {
            throw new IllegalStateException("session must be null");
        }
        // this will create completely new db instances and in turn update this handler through #init()
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
        helper = null;
        daoMaster = null;
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        ensureValid();
        return helper;
    }

    @Override
    public DaoSession getDaoSession() {
        ensureValid();
        return session;
    }

    @Override
    public SQLiteDatabase getDatabase() {
        ensureValid();
        return daoMaster.getDatabase();
    }
}
