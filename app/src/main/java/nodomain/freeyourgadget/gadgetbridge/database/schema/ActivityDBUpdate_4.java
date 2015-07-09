package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_STEPS_PER_DAY;

/**
 * Upgrade and downgrade with DB versions <= 5 is not supported.
 * Just recreates the default schema. Those GB versions may or may not
 * work with that, but this code will probably not create a DB for them
 * anyway.
 */
public class ActivityDBUpdate_4 extends ActivityDBCreationScript implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        recreateSchema(db);
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
        recreateSchema(db);
    }

    private void recreateSchema(SQLiteDatabase db) {
        DBHelper.dropTable(TABLE_GBACTIVITYSAMPLES, db);
        createSchema(db);
    }
}
