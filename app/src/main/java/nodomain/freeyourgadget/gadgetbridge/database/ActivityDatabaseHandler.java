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

    private static final int TYPE_ACTIVITY = 1;
    private static final int TYPE_LIGHT_SLEEP = 2;
    private static final int TYPE_DEEP_SLEEP = 4;
    private static final int TYPE_SLEEP = TYPE_LIGHT_SLEEP | TYPE_DEEP_SLEEP;
    private static final int TYPE_ALL = TYPE_ACTIVITY | TYPE_SLEEP;


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

    public ArrayList<GBActivitySample> getSleepSamples(int timestamp_from, int timestamp_to, byte provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, TYPE_SLEEP, provider);
    }

    public ArrayList<GBActivitySample> getActivitySamples(int timestamp_from, int timestamp_to, byte provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, TYPE_ACTIVITY, provider);
    }

    public ArrayList<GBActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to, byte provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, TYPE_ALL, provider);
    }

    /**
     * Returns all available activity samples from between the two timestamps (inclusive), of the given
     * provided and type(s).
     * @param timestamp_from
     * @param timestamp_to
     * @param activityTypes ORed combination of #TYPE_DEEP_SLEEP, #TYPE_LIGHT_SLEEP, #TYPE_ACTIVITY
     * @param provider
     * @return
     */
    private ArrayList<GBActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityTypes, byte provider) {
        if (timestamp_to == -1) {
            timestamp_to = Integer.MAX_VALUE; // dont know what happens when I use more than max of a signed int
        }
        ArrayList<GBActivitySample> GBActivitySampleList = new ArrayList<GBActivitySample>();
        final String where = "(provider=" + provider + " and timestamp>=" + timestamp_from + " and timestamp<=" + timestamp_to + ")";
        final String order = "timestamp";
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, null, where, null, null, null, order);

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
