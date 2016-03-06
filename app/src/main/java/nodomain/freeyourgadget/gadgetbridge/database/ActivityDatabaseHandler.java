package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.schema.ActivityDBCreationScript;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.impl.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.DATABASE_NAME;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_CUSTOM_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_INTENSITY;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_PROVIDER;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TIMESTAMP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TYPE;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;

public class ActivityDatabaseHandler extends SQLiteOpenHelper implements DBHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ActivityDatabaseHandler.class);

    private static final int DATABASE_VERSION = 7;

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
        LOG.info("ActivityDatabase: schema upgrade requested from " + oldVersion + " to " + newVersion);
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
        LOG.info("ActivityDatabase: schema downgrade requested from " + oldVersion + " to " + newVersion);
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

    public void addGBActivitySample(ActivitySample sample) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, sample.getTimestamp());
            values.put(KEY_PROVIDER, sample.getProvider().getID());
            values.put(KEY_INTENSITY, sample.getRawIntensity());
            values.put(KEY_STEPS, sample.getSteps());
            values.put(KEY_CUSTOM_SHORT, sample.getCustomValue());
            values.put(KEY_TYPE, sample.getRawKind());

            db.insert(TABLE_GBACTIVITYSAMPLES, null, values);
        }
    }

    /**
     * Adds the a new sample to the database
     *
     * @param timestamp        the timestamp of the same, second-based!
     * @param provider         the SampleProvider ID
     * @param intensity        the sample's raw intensity value
     * @param steps            the sample's steps value
     * @param kind             the raw activity kind of the sample
     * @param customShortValue
     */
    @Override
    public void addGBActivitySample(int timestamp, int provider, int intensity, int steps, int kind, int customShortValue) {
        if (intensity < 0) {
            LOG.error("negative intensity received, ignoring");
            intensity = 0;
        }
        if (steps < 0) {
            LOG.error("negative steps received, ignoring");
            steps = 0;
        }

        if (customShortValue < 0) {
            LOG.error("negative short value received, ignoring");
            customShortValue = 0;
        }

        try (SQLiteDatabase db = this.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(KEY_TIMESTAMP, timestamp);
            values.put(KEY_PROVIDER, provider);
            values.put(KEY_INTENSITY, intensity);
            values.put(KEY_STEPS, steps);
            values.put(KEY_TYPE, kind);
            values.put(KEY_CUSTOM_SHORT, customShortValue);

            db.insert(TABLE_GBACTIVITYSAMPLES, null, values);
        }
    }

    @Override
    public void addGBActivitySamples(ActivitySample[] activitySamples) {
        try (SQLiteDatabase db = this.getWritableDatabase()) {

            String sql = "INSERT INTO " + TABLE_GBACTIVITYSAMPLES + " (" + KEY_TIMESTAMP + "," +
                    KEY_PROVIDER + "," + KEY_INTENSITY + "," + KEY_STEPS + "," + KEY_TYPE + "," + KEY_CUSTOM_SHORT + ")" +
                    " VALUES (?,?,?,?,?,?);";
            SQLiteStatement statement = db.compileStatement(sql);
            db.beginTransaction();

            for (ActivitySample activitySample : activitySamples) {
                statement.clearBindings();
                statement.bindLong(1, activitySample.getTimestamp());
                statement.bindLong(2, activitySample.getProvider().getID());
                statement.bindLong(3, activitySample.getRawIntensity());
                statement.bindLong(4, activitySample.getSteps());
                statement.bindLong(5, activitySample.getRawKind());
                statement.bindLong(6, activitySample.getCustomValue());
                statement.execute();
            }
            db.setTransactionSuccessful();
            db.endTransaction();
        }
    }

    public ArrayList<ActivitySample> getSleepSamples(int timestamp_from, int timestamp_to, SampleProvider provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_SLEEP, provider);
    }

    public ArrayList<ActivitySample> getActivitySamples(int timestamp_from, int timestamp_to, SampleProvider provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ACTIVITY, provider);
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return this;
    }

    @Override
    public void release() {
        GBApplication.releaseDB();
    }

    public ArrayList<ActivitySample> getAllActivitySamples(int timestamp_from, int timestamp_to, SampleProvider provider) {
        return getGBActivitySamples(timestamp_from, timestamp_to, ActivityKind.TYPE_ALL, provider);
    }

    /**
     * Returns all available activity samples from between the two timestamps (inclusive), of the given
     * provided and type(s).
     *
     * @param timestamp_from
     * @param timestamp_to
     * @param activityTypes  ORed combination of #TYPE_DEEP_SLEEP, #TYPE_LIGHT_SLEEP, #TYPE_ACTIVITY
     * @param provider       the producer of the samples to be sought
     * @return
     */
    private ArrayList<ActivitySample> getGBActivitySamples(int timestamp_from, int timestamp_to, int activityTypes, SampleProvider provider) {
        if (timestamp_to < 0) {
            throw new IllegalArgumentException("negative timestamp_to");
        }
        if (timestamp_from < 0) {
            throw new IllegalArgumentException("negative timestamp_from");
        }
        ArrayList<ActivitySample> samples = new ArrayList<>();
        final String where = "(provider=" + provider.getID() + " and timestamp>=" + timestamp_from + " and timestamp<=" + timestamp_to + getWhereClauseFor(activityTypes, provider) + ")";
        LOG.info("Activity query where: " + where);
        final String order = "timestamp";
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, null, where, null, null, null, order)) {
                LOG.info("Activity query result: " + cursor.getCount() + " samples");
                if (cursor.moveToFirst()) {
                    do {
                        GBActivitySample sample = new GBActivitySample(
                                provider,
                                cursor.getInt(cursor.getColumnIndex(KEY_TIMESTAMP)),
                                cursor.getShort(cursor.getColumnIndex(KEY_INTENSITY)),
                                cursor.getShort(cursor.getColumnIndex(KEY_STEPS)),
                                (byte) cursor.getShort(cursor.getColumnIndex(KEY_TYPE)),
                                cursor.getShort(cursor.getColumnIndex(KEY_CUSTOM_SHORT)));
                        samples.add(sample);
                    } while (cursor.moveToNext());
                }
            }
        }

        return samples;
    }

    private String getWhereClauseFor(int activityTypes, SampleProvider provider) {
        if (activityTypes == ActivityKind.TYPE_ALL) {
            return ""; // no further restriction
        }

        StringBuilder builder = new StringBuilder(" and (");
        int[] dbActivityTypes = ActivityKind.mapToDBActivityTypes(activityTypes, provider);
        for (int i = 0; i < dbActivityTypes.length; i++) {
            builder.append(" type=").append(dbActivityTypes[i]);
            if (i + 1 < dbActivityTypes.length) {
                builder.append(" or ");
            }
        }
        builder.append(')');
        return builder.toString();
    }

    @Override
    public void changeStoredSamplesType(int timestampFrom, int timestampTo, int kind, SampleProvider provider) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            String sql = "UPDATE " + TABLE_GBACTIVITYSAMPLES + " SET " + KEY_TYPE + "= ? WHERE "
                    + KEY_PROVIDER + " = ? AND "
                    + KEY_TIMESTAMP + " >= ? AND " + KEY_TIMESTAMP + " < ? ;"; //do not use BETWEEN because the range is inclusive in that case!

            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, kind);
            statement.bindLong(2, provider.getID());
            statement.bindLong(3, timestampFrom);
            statement.bindLong(4, timestampTo);
            statement.execute();
        }
    }

    @Override
    public int fetchLatestTimestamp(SampleProvider provider) {
        try (SQLiteDatabase db = this.getReadableDatabase()) {
            try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, new String[]{KEY_TIMESTAMP}, KEY_PROVIDER + "=" + String.valueOf(provider.getID()), null, null, null, KEY_TIMESTAMP + " DESC", "1")) {
                if (cursor.moveToFirst()) {
                    return cursor.getInt(0);
                }
            }
        }
        return -1;
    }
}
