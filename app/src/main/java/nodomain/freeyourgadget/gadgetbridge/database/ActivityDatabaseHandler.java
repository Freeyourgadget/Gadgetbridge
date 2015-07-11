package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GB;
import nodomain.freeyourgadget.gadgetbridge.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.schema.ActivityDBCreationScript;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.*;

public class ActivityDatabaseHandler extends SQLiteOpenHelper {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityDatabaseHandler.class);

    private static final int DATABASE_VERSION = 5;

    public ActivityDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            ActivityDBCreationScript script = new ActivityDBCreationScript();
            script.createSchema(db);
        } catch (RuntimeException ex) {
            GB.toast("Error creating database.", Toast.LENGTH_SHORT, GB.ERROR, ex);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            for (int i = oldVersion + 1; i <= newVersion; i++) {
                DBUpdateScript updater = getUpdateScript(db, i);
                if (updater != null) {
                    LOG.info("upgrading activity database to version " + i);
                    updater.upgradeSchema(db);
                }
            }
            LOG.info("activity database is now at version " + newVersion);
        } catch (RuntimeException ex) {
            GB.toast("Error upgrading database.", Toast.LENGTH_SHORT, GB.ERROR, ex);
            throw ex; // reject upgrade
        }
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            for (int i = oldVersion; i >= newVersion; i--) {
                DBUpdateScript updater = getUpdateScript(db, i);
                if (updater != null) {
                    LOG.info("downgrading activity database to version " + (i - 1));
                    updater.downgradeSchema(db);
                }
            }
            LOG.info("activity database is now at version " + newVersion);
        } catch (RuntimeException ex) {
            GB.toast("Error downgrading database.", Toast.LENGTH_SHORT, GB.ERROR, ex);
            throw ex; // reject downgrade
        }
    }

    private DBUpdateScript getUpdateScript(SQLiteDatabase db, int version) {
        try {
            Class<?> updateClass = getClass().getClassLoader().loadClass(getClass().getPackage().getName() + ".schema.ActivityDBUpdate_" + version);
            return (DBUpdateScript) updateClass.newInstance();
        } catch (ClassNotFoundException e) {
            return null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException("Error instantiating DBUpdate class for version " + version, e);
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

        try (SQLiteDatabase db = this.getReadableDatabase()) {
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
