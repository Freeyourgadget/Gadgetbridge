package nodomain.freeyourgadget.gadgetbridge.devices.qhybrid;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.ArrayList;

import nodomain.freeyourgadget.gadgetbridge.database.DBOpenHelper;

public class PackageConfigHelper extends DBOpenHelper {
    public static final String DB_NAME = "qhybridNotifications.db";
    public static final String DB_ID = "id";
    public static final String DB_TABLE = "notifications";
    public static final String DB_PACKAGE = "package";
    public static final String DB_APPNAME = "appName";
    public static final String DB_VIBRATION = "vibrationTtype";
    public static final String DB_MINUTE = "minDegress";
    public static final String DB_HOUR = "hourDegrees";
    public static final String DB_RESPECT_SILENT = "respectSilent";

    SQLiteDatabase database;


    public PackageConfigHelper(Context context) {
        super(context, DB_NAME, null);
        this.database = getWritableDatabase();
        initDB();
    }

    public void saveConfig(PackageConfig settings){
        ContentValues values = new ContentValues(6);
        values.put(DB_PACKAGE, settings.getPackageName());
        values.put(DB_APPNAME, settings.getAppName());
        values.put(DB_HOUR, settings.getHour());
        values.put(DB_MINUTE, settings.getMin());
        values.put(DB_VIBRATION, settings.getVibration());
        values.put(DB_RESPECT_SILENT, settings.getRespectSilentMode());

        if(settings.getId() == -1) {
            settings.setId(database.insert(DB_TABLE, null, values));
        }else{
            database.update(DB_TABLE, values, DB_ID + "=?", new String[]{String.valueOf(settings.getId())});
        }
        //LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent());
    }

    public ArrayList<PackageConfig> getSettings(){
        Cursor cursor = database.query(DB_TABLE, new String[]{"*"}, null, null, null, null, null);
        int size = cursor.getCount();
        ArrayList<PackageConfig> list = new ArrayList<>(size);
        if(size > 0){
            int appNamePos = cursor.getColumnIndex(DB_APPNAME);
            int packageNamePos = cursor.getColumnIndex(DB_PACKAGE);
            int hourPos = cursor.getColumnIndex(DB_HOUR);
            int minPos = cursor.getColumnIndex(DB_MINUTE);
            int silentPos = cursor.getColumnIndex(DB_RESPECT_SILENT);
            int vibrationPos = cursor.getColumnIndex(DB_VIBRATION);
            int idPos = cursor.getColumnIndex(DB_ID);
            cursor.moveToFirst();
            do {
                list.add(new PackageConfig(
                        cursor.getInt(minPos),
                        cursor.getInt(hourPos),
                        cursor.getString(packageNamePos),
                        cursor.getString(appNamePos),
                        cursor.getInt(silentPos) == 1,
                        cursor.getInt(vibrationPos),
                        cursor.getInt(idPos)
                ));
                Log.d("Settings", "setting #" + cursor.getPosition() + ": " + cursor.getInt(silentPos));
            }while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public PackageConfig getSetting(String appName){
        Cursor c = database.query(DB_TABLE, new String[]{"*"}, DB_APPNAME + "=?", new String[]{appName}, null, null, null);
        if(c.getCount() == 0){
            c.close();
            return null;
        }
        c.moveToFirst();
        PackageConfig settings = new PackageConfig(
                c.getInt(c.getColumnIndex(DB_MINUTE)),
                c.getInt(c.getColumnIndex(DB_HOUR)),
                c.getString(c.getColumnIndex(DB_PACKAGE)),
                c.getString(c.getColumnIndex(DB_APPNAME)),
                c.getInt(c.getColumnIndex(DB_RESPECT_SILENT)) == 1,
                c.getInt(c.getColumnIndex(DB_VIBRATION)),
                c.getInt(c.getColumnIndex(DB_ID))
        );
        c.close();
        return settings;
    }

    private void initDB(){
        database.execSQL("CREATE TABLE IF NOT EXISTS notifications(" +
                DB_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                DB_PACKAGE + " TEXT, " +
                DB_VIBRATION + " INTEGER, " +
                DB_MINUTE + " INTEGER DEFAULT -1, " +
                DB_APPNAME + " TEXT," +
                DB_RESPECT_SILENT + " INTEGER," +
                DB_HOUR + " INTEGER DEFAULT -1);");
    }

    @Override
    public void close(){
        super.close();
        database.close();
    }

    public void deleteConfig(PackageConfig packageSettings) {
        Log.d("DB", "deleting id " + packageSettings.getId());
        if(packageSettings.getId() == -1) return;
        this.database.delete(DB_TABLE, DB_ID + "=?", new String[]{String.valueOf(packageSettings.getId())});
    }
}
