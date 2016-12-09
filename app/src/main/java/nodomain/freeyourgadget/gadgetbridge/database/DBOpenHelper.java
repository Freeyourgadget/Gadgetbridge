package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.schema.SchemaMigration;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;

public class DBOpenHelper extends DaoMaster.OpenHelper {
    private final String updaterClassNamePrefix;
    private final Context context;

    public DBOpenHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory) {
        super(context, dbName, factory);
        updaterClassNamePrefix = dbName + "Update_";
        this.context = context;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DaoMaster.createAllTables(db, true);
        new SchemaMigration(updaterClassNamePrefix).onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        DaoMaster.createAllTables(db, true);
        new SchemaMigration(updaterClassNamePrefix).onDowngrade(db, oldVersion, newVersion);
    }

    public Context getContext() {
        return context;
    }
}
