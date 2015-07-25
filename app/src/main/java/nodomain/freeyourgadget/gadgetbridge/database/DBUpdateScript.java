package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;

/**
 * Interface for updating a database schema.
 * Implementors provide the update from the prior schema
 * version to this version, and the downgrade from this schema
 * version to the next lower version.
 * <p/>
 * Implementations must have a public, no-arg constructor.
 */
public interface DBUpdateScript {
    void upgradeSchema(SQLiteDatabase database);

    void downgradeSchema(SQLiteDatabase database);
}
