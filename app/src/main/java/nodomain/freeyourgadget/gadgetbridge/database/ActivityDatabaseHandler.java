package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.io.File;

import nodomain.freeyourgadget.gadgetbridge.database.schema.ActivityDBCreationScript;
import nodomain.freeyourgadget.gadgetbridge.database.schema.SchemaMigration;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.DATABASE_NAME;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TIMESTAMP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;

/**
 * @deprecated can be removed entirely, only used for backwards compatibility
 */
public class ActivityDatabaseHandler extends SQLiteOpenHelper implements DBHandler {

    private static final int DATABASE_VERSION = 7;
    private static final String UPDATER_CLASS_NAME_PREFIX = "ActivityDBUpdate_";
    private final Context context;

    public ActivityDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
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
        new SchemaMigration(UPDATER_CLASS_NAME_PREFIX).onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration(UPDATER_CLASS_NAME_PREFIX).onDowngrade(db, oldVersion, newVersion);
    }

    @Override
    public SQLiteDatabase getDatabase() {
        return super.getWritableDatabase();
    }

    @Override
    public void closeDb() {
    }

    @Override
    public void openDb() {
    }

    @Override
    public SQLiteOpenHelper getHelper() {
        return this;
    }

    public Context getContext() {
        return context;
    }

    public boolean hasContent() {
        File dbFile = getContext().getDatabasePath(getDatabaseName());
        if (dbFile == null || !dbFile.exists()) {
            return false;
        }

        try {
            try (SQLiteDatabase db = this.getReadableDatabase()) {
                try (Cursor cursor = db.query(TABLE_GBACTIVITYSAMPLES, new String[]{KEY_TIMESTAMP}, null, null, null, null, null, "1")) {
                    return cursor.moveToFirst();
                }
            }
        } catch (Exception ex) {
            // can't expect anything
            GB.log("Error looking for old activity data: " + ex.getMessage(), GB.ERROR, ex);
            return false;
        }
    }

    @Override
    public DaoSession getDaoSession() {
        throw new UnsupportedOperationException();
    }

    @Override
    public DaoMaster getDaoMaster() {
        throw new UnsupportedOperationException();
    }
}
