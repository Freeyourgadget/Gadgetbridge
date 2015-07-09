package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;

public class DBHelper {
    private final Context context;

    public DBHelper(Context context) {
        this.context = context;
    }

    private String getClosedDBPath(SQLiteOpenHelper dbHandler) throws IllegalStateException {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        String path = db.getPath();
        db.close(); // reference counted, so may still be open
        if (db.isOpen()) {
            throw new IllegalStateException("Database must be closed");
        }
        return path;
    }

    public File exportDB(SQLiteOpenHelper dbHandler, File toDir) throws IllegalStateException, IOException {
        String dbPath = getClosedDBPath(dbHandler);
        File sourceFile = new File(dbPath);
        File destFile = new File(toDir, sourceFile.getName());
        if (destFile.exists()) {
            File backup = new File(toDir, destFile.getName() + "_" + getDate());
            destFile.renameTo(backup);
        } else if (!toDir.exists()) {
            if (!toDir.mkdirs()) {
                throw new IOException("Unable to create directory: " + toDir.getAbsolutePath());
            }
        }

        FileUtils.copyFile(sourceFile, destFile);
        return destFile;
    }

    private String getDate() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    }

    public void importDB(SQLiteOpenHelper dbHandler, File fromFile) throws IllegalStateException, IOException {
        String dbPath = getClosedDBPath(dbHandler);
        File toFile = new File(dbPath);
        FileUtils.copyFile(fromFile, toFile);
    }

    public void validateDB(SQLiteOpenHelper dbHandler) throws IOException {
        try (SQLiteDatabase db = dbHandler.getReadableDatabase()) {
            if (!db.isDatabaseIntegrityOk()) {
                throw new IOException("Database integrity is not OK");
            }
        }
    }

    public static void dropTable(String tableName, SQLiteDatabase db) {
        db.delete(tableName, null, null);
    }

    /**
     * WITHOUT ROWID is only available with sqlite 3.8.2, which is available
     * with Lollipop and later.
     *
     * @return the "WITHOUT ROWID" string or an empty string for pre-Lollipop devices
     */
    public static String getWithoutRowId() {
        if (GBApplication.isRunningLollipopOrLater()) {
            return " WITHOUT ROWID;";
        }
        return "";
    }


}
