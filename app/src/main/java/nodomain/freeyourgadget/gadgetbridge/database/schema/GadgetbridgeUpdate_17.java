package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;
import nodomain.freeyourgadget.gadgetbridge.entities.No1F1ActivitySampleDao;

public class GadgetbridgeUpdate_17 implements DBUpdateScript {
    @Override
    public void upgradeSchema(SQLiteDatabase db) {
        if (!DBHelper.existsColumn(No1F1ActivitySampleDao.TABLENAME, No1F1ActivitySampleDao.Properties.HeartRate.columnName, db)) {
            String ADD_COLUMN_HEART_RATE = "ALTER TABLE " + No1F1ActivitySampleDao.TABLENAME + " ADD COLUMN "
                    + No1F1ActivitySampleDao.Properties.HeartRate.columnName + " INTEGER NOT NULL DEFAULT 0;";
            db.execSQL(ADD_COLUMN_HEART_RATE);
        }
    }

    @Override
    public void downgradeSchema(SQLiteDatabase db) {
    }
}
