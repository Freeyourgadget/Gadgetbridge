package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivitySampleDao;

/*
 * adds heart rate column to health table
 */

public class GadgetbridgeUpdate_14 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(PebbleHealthActivitySampleDao.TABLENAME, PebbleHealthActivitySampleDao.Properties.HeartRate.columnName, db)) {
            String ADD_COLUMN_HEART_RATE = "ALTER TABLE " + PebbleHealthActivitySampleDao.TABLENAME + " ADD COLUMN "
                    + PebbleHealthActivitySampleDao.Properties.HeartRate.columnName + " INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(ADD_COLUMN_HEART_RATE);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
