package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.LockHandler;
import nodomain.freeyourgadget.gadgetbridge.database.schema.SchemaMigration;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.SampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.impl.GBActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.HeartRateSample;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_CUSTOM_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_INTENSITY;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_PROVIDER;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TIMESTAMP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TYPE;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;

public class DBOpenHelper extends DaoMaster.OpenHelper {
    private final String updaterClassNamePrefix;
    private final Context context;

    public DBOpenHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory) {
        super(context, dbName, factory);
        updaterClassNamePrefix = dbName + "Update_";
        this.context = context;
    }

    public boolean importOldDbIfNecessary(DaoMaster daoMaster, DBHandler targetDBHandler) {
        DaoSession tempSession = daoMaster.newSession();
        try {
            if (isEmpty(tempSession)) {
                importActivityDatabaseInto(tempSession, targetDBHandler);
                return true;
            }
        } finally {
            tempSession.clear();
        }
        return false;
    }

    private boolean isEmpty(DaoSession session) {
        long totalSamplesCount = session.getMiBandActivitySampleDao().count();
        totalSamplesCount += session.getPebbleActivitySampleDao().count();
        return totalSamplesCount == 0;
    }

    private void importActivityDatabaseInto(DaoSession session, DBHandler targetDBHandler) {
        ActivityDatabaseHandler handler = new ActivityDatabaseHandler(getContext());

        try (SQLiteDatabase db = handler.getReadableDatabase()) {
            User user = DBHelper.getUser(session);
            for (DeviceCoordinator coordinator : DeviceHelper.getInstance().getAllCoordinators()) {
                AbstractSampleProvider<? extends AbstractActivitySample> sampleProvider = (AbstractSampleProvider<? extends AbstractActivitySample>) coordinator.getSampleProvider(targetDBHandler);
                importActivitySamples(db, session, sampleProvider, user);
            }
        }
    }

    private <T extends AbstractActivitySample> void importActivitySamples(SQLiteDatabase fromDb, DaoSession targetSession, AbstractSampleProvider<T> sampleProvider, User user) {
        String order = "timestamp";
        final String where = "provider=" + sampleProvider.getID();

        try (Cursor cursor = fromDb.query(TABLE_GBACTIVITYSAMPLES, null, where, null, null, null, order)) {
            int colTimeStamp = cursor.getColumnIndex(KEY_TIMESTAMP);
            int colIntensity = cursor.getColumnIndex(KEY_INTENSITY);
            int colSteps = cursor.getColumnIndex(KEY_STEPS);
            int colType = cursor.getColumnIndex(KEY_TYPE);
            int colCustomShort = cursor.getColumnIndex(KEY_CUSTOM_SHORT);
            Long userId = user.getId();
            List<T> newSamples = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                T newSample = sampleProvider.createActivitySample();
                newSample.setUserId(userId);
                newSample.setDeviceId(deviceId);
                newSample.setTimestamp(cursor.getInt(colTimeStamp));
                newSample.setRawKind(cursor.getInt(colType));
                newSample.setProvider(sampleProvider);
                newSample.setRawIntensity(cursor.getInt(colIntensity));
                newSample.setSteps(cursor.getInt(colSteps));

                int hrValue = cursor.getInt(colCustomShort);
                if (newSample instanceof HeartRateSample) {
                    ((HeartRateSample)newSample).setHeartRate(hrValue);
                }
                newSamples.add(newSample);
            }
            sampleProvider.getSampleDao().insertInTx(newSamples, true);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration(updaterClassNamePrefix).onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration(updaterClassNamePrefix).onDowngrade(db, oldVersion, newVersion);
    }

    public Context getContext() {
        return context;
    }
}
