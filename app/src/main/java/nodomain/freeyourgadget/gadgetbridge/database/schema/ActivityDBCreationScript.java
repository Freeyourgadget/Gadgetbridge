package nodomain.freeyourgadget.gadgetbridge.database.schema;

import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.database.DBUpdateScript;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.*;

public class ActivityDBCreationScript {
    public void createSchema(SQLiteDatabase db) {
        String CREATE_GBACTIVITYSAMPLES_TABLE = "CREATE TABLE " + TABLE_GBACTIVITYSAMPLES + " ("
                + KEY_TIMESTAMP + " INT,"
                + KEY_PROVIDER + " TINYINT,"
                + KEY_INTENSITY + " SMALLINT,"
                + KEY_STEPS + " TINYINT,"
                + KEY_TYPE + " TINYINT,"
                + " PRIMARY KEY (" + KEY_TIMESTAMP + "," + KEY_PROVIDER + ") ON CONFLICT REPLACE)" + DBHelper.getWithoutRowId();
        db.execSQL(CREATE_GBACTIVITYSAMPLES_TABLE);
    }
}
