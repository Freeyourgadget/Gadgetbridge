package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;

public interface DBHandler extends AutoCloseable {
    /**
     * Closes the database.
     */
    void closeDb();
    void openDb();

    SQLiteOpenHelper getHelper();

    /**
     * Releases the DB handler. No access may be performed after calling this method.
     * Same as calling {@link GBApplication#releaseDB()}
     */
    void close() throws Exception;

    SQLiteDatabase getDatabase();

    DaoSession getDaoSession();
}
