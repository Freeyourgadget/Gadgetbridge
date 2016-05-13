package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.greenrobot.dao.query.Query;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.UserDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;

public class DBHelper {
    private final Context context;

    public DBHelper(Context context) {
        this.context = context;
    }

    private String getClosedDBPath(SQLiteOpenHelper dbHandler) throws IllegalStateException {
        SQLiteDatabase db = dbHandler.getReadableDatabase();
        String path = db.getPath();
        db.close();
        if (db.isOpen()) { // reference counted, so may still be open
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
        String statement = "DROP TABLE IF EXISTS '" + tableName + "'";
        db.execSQL(statement);
    }

    public static boolean existsColumn(String tableName, String columnName, SQLiteDatabase db) {
        try (Cursor res = db.rawQuery("PRAGMA table_info('" + tableName + "')", null)) {
            int index = res.getColumnIndex("name");
            if (index < 1) {
                return false; // something's really wrong
            }
            while (res.moveToNext()) {
                String cn = res.getString(index);
                if (columnName.equals(cn)) {
                    return true;
                }
            }
        }
        return false;
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

    public static User getUser() {
        DaoSession session = GBApplication.getDaoSession();
        UserDao userDao = session.getUserDao();
        List<User> users = userDao.loadAll();
        if (users.isEmpty()) {
            User user = createUser(session);
            return user;
        }
        return users.get(0); // TODO: multiple users support?
    }

    private static User createUser(DaoSession session) {
        ActivityUser prefsUser = new ActivityUser();
        User user = new User();
        user.setName(prefsUser.getName());
        user.setBirthday(prefsUser.getUserBirthday());
        user.setGender(prefsUser.getGender());
        session.getUserDao().insert(user);
        List<UserAttributes> userAttributes = user.getUserAttributesList();

        UserAttributes attributes = new UserAttributes();
        attributes.setValidFromUTC(DateTimeUtils.todayUTC());
        attributes.setHeightCM(prefsUser.getHeightCm());
        attributes.setWeightKG(prefsUser.getWeightKg());
        attributes.setUserId(user.getId());
        session.getUserAttributesDao().insert(attributes);

        userAttributes.add(attributes);

        return user;
    }

    public static Device getDevice(GBDevice gbDevice) {
        DaoSession session = GBApplication.getDaoSession();
        DeviceDao deviceDao = session.getDeviceDao();
        Query<Device> query = deviceDao.queryBuilder().where(DeviceDao.Properties.Identifier.eq(gbDevice.getAddress())).build();
        List<Device> devices = query.list();
        if (devices.isEmpty()) {
            Device device = createDevice(session, gbDevice);
            return device;
        }
        return devices.get(0);
    }

    private static Device createDevice(DaoSession session, GBDevice gbDevice) {
        Device device = new Device();
        device.setIdentifier(gbDevice.getAddress());
        device.setName(gbDevice.getName());
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        device.setManufacturer(coordinator.getManufacturer());
        session.getDeviceDao().insert(device);
        List<DeviceAttributes> deviceAttributes = device.getDeviceAttributesList();

        DeviceAttributes attributes = new DeviceAttributes();
        attributes.setDeviceId(device.getId());
        attributes.setValidFromUTC(DateTimeUtils.todayUTC());
        attributes.setFirmwareVersion1(gbDevice.getFirmwareVersion());
        // TODO: firmware version2? generically or through DeviceCoordinator?
        DeviceAttributesDao attributesDao = session.getDeviceAttributesDao();
        attributesDao.insert(attributes);

        deviceAttributes.add(attributes);

        return device;
    }
}
