package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;

public class ActivityDatabaseHandler extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 5;

    private static final String DATABASE_NAME = "ActivityDatabase";

    private static final String TABLE_GBACTIVITYSAMPLES = "GBActivitySamples";

    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_PROVIDER = "provider";
    private static final String KEY_INTENSITY = "intensity";
    private static final String KEY_STEPS = "steps";
    private static final String KEY_TYPE = "type";

    public ActivityDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_GBACTIVITYSAMPLES_TABLE = "CREATE TABLE " + TABLE_GBACTIVITYSAMPLES + " ("
                + KEY_TIMESTAMP + " INT,"
                + KEY_PROVIDER + " TINYINT,"
                + KEY_INTENSITY + " SMALLINT,"
                + KEY_STEPS + " TINYINT,"
                + KEY_TYPE + " TINYINT,"
                + " PRIMARY KEY (" + KEY_TIMESTAMP + "," + KEY_PROVIDER + ") ON CONFLICT REPLACE)" + getWithoutRowId();
        db.execSQL(CREATE_GBACTIVITYSAMPLES_TABLE);
    }

    /**
     * WITHOUT ROWID is only available with sqlite 3.8.2, which is available
     * with Lollipop and later.
     * @return the "WITHOUT ROWID" string or an empty string for pre-Lollipop devices
     */
    private String getWithoutRowId() {
        if (GBApplication.isRunningLollipopOrLater()) {
            return " WITHOUT ROWID;";
        }
        return "";
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (newVersion == 5 && (oldVersion == 4 || oldVersion ==3)) {
            String CREATE_NEW_GBACTIVITYSAMPLES_TABLE = "CREATE TABLE NEW ("
                    + KEY_TIMESTAMP + " INT,"
                    + KEY_PROVIDER + " TINYINT,"
                    + KEY_INTENSITY + " SMALLINT,"
                    + KEY_STEPS + " TINYINT,"
                    + KEY_TYPE + " TINYINT,"
                    + " PRIMARY KEY (" + KEY_TIMESTAMP + "," + KEY_PROVIDER + ") ON CONFLICT REPLACE)" + getWithoutRowId();
            db.execSQL(CREATE_NEW_GBACTIVITYSAMPLES_TABLE);
            db.execSQL("insert into NEW select timestamp,provider,intensity,steps,type from "+ TABLE_GBACTIVITYSAMPLES+";");
            db.execSQL("Drop table "+TABLE_GBACTIVITYSAMPLES+";");
            db.execSQL("alter table NEW RENAME TO " + TABLE_GBACTIVITYSAMPLES + ";");
        } else {
            //FIXME: do not just recreate
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_GBACTIVITYSAMPLES);
            onCreate(db);
        }
    }


    public void addGBActivitySample(GBActivitySample GBActivitySample) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, GBActivitySample.getTimestamp());
            values.put(KEY_PROVIDER, GBActivitySample.getProvider());
            values.put(KEY_INTENSITY, GBActivitySample.getIntensity());
            values.put(KEY_STEPS, GBActivitySample.getSteps());
            values.put(KEY_TYPE, GBActivitySample.getType());

            db.insert(TABLE_GBACTIVITYSAMPLES, null, values);
        }
    }

    public void addGBActivitySample(int timestamp, byte provider, short intensity, byte steps, byte type) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timestamp);
            values.put(KEY_PROVIDER, provider);
            values.put(KEY_INTENSITY, intensity);
            values.put(KEY_STEPS, steps);
            values.put(KEY_TYPE, type);

            db.insert(TABLE_GBACTIVITYSAMPLES, null, values);
        }
    }

    public ArrayList<GBActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to, byte provider) {
        if (timestamp_to == -1) {
            timestamp_to = Integer.MAX_VALUE; // dont know what happens when I use more than max of a signed int
        }
        ArrayList<GBActivitySample> GBActivitySampleList = new ArrayList<GBActivitySample>();
        String selectQuery = "SELECT  * FROM " + TABLE_GBACTIVITYSAMPLES
                + " where (provider=" + provider + " and timestamp>=" + timestamp_from + " and timestamp<=" + timestamp_to + ") order by timestamp";

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            Cursor cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    GBActivitySample GBActivitySample = new GBActivitySample(
                            cursor.getInt(cursor.getColumnIndex(KEY_TIMESTAMP)),
                            (byte) cursor.getInt(cursor.getColumnIndex(KEY_PROVIDER)),
                            (short) cursor.getInt(cursor.getColumnIndex(KEY_INTENSITY)),
                            (byte) cursor.getInt(cursor.getColumnIndex(KEY_STEPS)),
                            (byte) cursor.getInt(cursor.getColumnIndex(KEY_TYPE)));
                    GBActivitySampleList.add(GBActivitySample);
                } while (cursor.moveToNext());
            }
        }

        return GBActivitySampleList;
    }
}
