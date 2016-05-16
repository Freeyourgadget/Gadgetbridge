package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import nodomain.freeyourgadget.gadgetbridge.database.schema.SchemaMigration;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;

public class DBOpenHelper extends DaoMaster.OpenHelper {
    public DBOpenHelper(Context context, String dbName, SQLiteDatabase.CursorFactory factory) {
        super(context, dbName, factory);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration().onUpgrade(db, oldVersion, newVersion);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        new SchemaMigration().onDowngrade(db, oldVersion, newVersion);
    }
}
