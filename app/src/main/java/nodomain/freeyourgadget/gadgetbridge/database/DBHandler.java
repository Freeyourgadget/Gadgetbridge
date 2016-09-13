package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

/**
 * Provides lowlevel access to the database.
 */
public interface DBHandler extends AutoCloseable {
    /**
     * Closes the database.
     */
    void closeDb();

    /**
     * Opens the database. Note that this is only possible after an explicit
     * #closeDb(). Initially the db is implicitly open.
     */
    void openDb();

    SQLiteOpenHelper getHelper();

    /**
     * Releases the DB handler. No DB access will be possible before
     * #openDb() will be called.
     */
    void close() throws Exception;

    SQLiteDatabase getDatabase();

    DaoMaster getDaoMaster();
    DaoSession getDaoSession();
}
