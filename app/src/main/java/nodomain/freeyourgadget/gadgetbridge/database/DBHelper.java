/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Felix Konstantin Maurer, JohnnySun

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst;
import nodomain.freeyourgadget.gadgetbridge.entities.ActivityDescription;
import nodomain.freeyourgadget.gadgetbridge.entities.ActivityDescriptionDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Alarm;
import nodomain.freeyourgadget.gadgetbridge.entities.AlarmDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Tag;
import nodomain.freeyourgadget.gadgetbridge.entities.TagDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.UserDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.ValidByDate;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;


/**
 * Provides utiliy access to some common entities, so you won't need to use
 * their DAO classes.
 * <p/>
 * Maybe this code should actually be in the DAO classes themselves, but then
 * these should be under revision control instead of 100% generated at build time.
 */
public class DBHelper {
    private static final Logger LOG = LoggerFactory.getLogger(DBHelper.class);

    private final Context context;

    public DBHelper(Context context) {
        this.context = context;
    }

    /**
     * Closes the database and returns its name.
     * Important: after calling this, you have to DBHandler#openDb() it again
     * to get it back to work.
     *
     * @param dbHandler
     * @return
     * @throws IllegalStateException
     */
    private String getClosedDBPath(DBHandler dbHandler) throws IllegalStateException {
        SQLiteDatabase db = dbHandler.getDatabase();
        String path = db.getPath();
        dbHandler.closeDb();
        if (db.isOpen()) { // reference counted, so may still be open
            throw new IllegalStateException("Database must be closed");
        }
        return path;
    }

    public File exportDB(DBHandler dbHandler, File toDir) throws IllegalStateException, IOException {
        String dbPath = getClosedDBPath(dbHandler);
        try {
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
        } finally {
            dbHandler.openDb();
        }
    }

    public void exportDB(DBHandler dbHandler, OutputStream dest) throws IOException {
        String dbPath = getClosedDBPath(dbHandler);
        try {
            File source = new File(dbPath);
            FileUtils.copyFileToStream(source, dest);
        } finally {
            dbHandler.openDb();
        }
    }

    private String getDate() {
        return new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
    }

    public void importDB(DBHandler dbHandler, File fromFile) throws IllegalStateException, IOException {
        String dbPath = getClosedDBPath(dbHandler);
        try {
            File toFile = new File(dbPath);
            FileUtils.copyFile(fromFile, toFile);
        } finally {
            dbHandler.openDb();
        }
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

    public boolean existsDB(String dbName) {
        File path = context.getDatabasePath(dbName);
        return path != null && path.exists();
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
    @NonNull
    public static String getWithoutRowId() {
        if (GBApplication.isRunningLollipopOrLater()) {
            return " WITHOUT ROWID;";
        }
        return "";
    }

    /**
     * Looks up the user entity in the database. If a user exists already, it will
     * be updated with the current preferences values. If no user exists yet, it will
     * be created in the database.
     *
     * Note: so far there is only ever a single user; there is no multi-user support yet
     * @param session
     * @return the User entity
     */
    @NonNull
    public static User getUser(DaoSession session) {
        ActivityUser prefsUser = new ActivityUser();
        UserDao userDao = session.getUserDao();
        User user;
        List<User> users = userDao.loadAll();
        if (users.isEmpty()) {
            user = createUser(prefsUser, session);
        } else {
            user = users.get(0); // TODO: multiple users support?
            ensureUserUpToDate(user, prefsUser, session);
        }
        ensureUserAttributes(user, prefsUser, session);

        return user;
    }

    @NonNull
    public static UserAttributes getUserAttributes(User user) {
        List<UserAttributes> list = user.getUserAttributesList();
        if (list.isEmpty()) {
            throw new IllegalStateException("user has no attributes");
        }
        return list.get(0);
    }

    @NonNull
    private static User createUser(ActivityUser prefsUser, DaoSession session) {
        User user = new User();
        ensureUserUpToDate(user, prefsUser, session);

        return user;
    }

    private static void ensureUserUpToDate(User user, ActivityUser prefsUser, DaoSession session) {
        if (!isUserUpToDate(user, prefsUser)) {
            user.setName(prefsUser.getName());
            user.setBirthday(prefsUser.getUserBirthday());
            user.setGender(prefsUser.getGender());

            if (user.getId() == null) {
                session.getUserDao().insert(user);
            } else {
                session.getUserDao().update(user);
            }
        }
    }

    public static boolean isUserUpToDate(User user, ActivityUser prefsUser) {
        if (!Objects.equals(user.getName(), prefsUser.getName())) {
            return false;
        }
        if (!Objects.equals(user.getBirthday(), prefsUser.getUserBirthday())) {
            return false;
        }
        if (user.getGender() != prefsUser.getGender()) {
            return false;
        }

        return true;
    }

    private static void ensureUserAttributes(User user, ActivityUser prefsUser, DaoSession session) {
        List<UserAttributes> userAttributes = user.getUserAttributesList();
        UserAttributes[] previousUserAttributes = new UserAttributes[1];
        if (hasUpToDateUserAttributes(userAttributes, prefsUser, previousUserAttributes)) {
            return;
        }

        Calendar now = DateTimeUtils.getCalendarUTC();
        invalidateUserAttributes(previousUserAttributes[0], now, session);

        UserAttributes attributes = new UserAttributes();
        attributes.setValidFromUTC(now.getTime());
        attributes.setHeightCM(prefsUser.getHeightCm());
        attributes.setWeightKG(prefsUser.getWeightKg());
        attributes.setSleepGoalHPD(prefsUser.getSleepDuration());
        attributes.setStepsGoalSPD(prefsUser.getStepsGoal());
        attributes.setUserId(user.getId());
        session.getUserAttributesDao().insert(attributes);

// sort order is important, so we re-fetch from the db
//        userAttributes.add(attributes);
        user.resetUserAttributesList();
    }

    private static void invalidateUserAttributes(UserAttributes userAttributes, Calendar now, DaoSession session) {
        if (userAttributes != null) {
            Calendar invalid = (Calendar) now.clone();
            invalid.add(Calendar.MINUTE, -1);
            userAttributes.setValidToUTC(invalid.getTime());
            session.getUserAttributesDao().update(userAttributes);
        }
    }

    private static boolean hasUpToDateUserAttributes(List<UserAttributes> userAttributes, ActivityUser prefsUser, UserAttributes[] outPreviousUserAttributes) {
        for (UserAttributes attr : userAttributes) {
            if (!isValidNow(attr)) {
                continue;
            }
            if (isEqual(attr, prefsUser)) {
                return true;
            } else {
                outPreviousUserAttributes[0] = attr;
            }
        }
        return false;
    }

    // TODO: move this into db queries?
    private static boolean isValidNow(ValidByDate element) {
        Calendar cal = DateTimeUtils.getCalendarUTC();
        Date nowUTC = cal.getTime();
        return isValid(element, nowUTC);
    }

    private static boolean isValid(ValidByDate element, Date nowUTC) {
        Date validFromUTC = element.getValidFromUTC();
        Date validToUTC = element.getValidToUTC();
        if (nowUTC.before(validFromUTC)) {
            return false;
        }
        if (validToUTC != null && nowUTC.after(validToUTC)) {
            return false;
        }
        return true;
    }

    private static boolean isEqual(UserAttributes attr, ActivityUser prefsUser) {
        if (prefsUser.getHeightCm() != attr.getHeightCM()) {
            LOG.info("user height changed to " + prefsUser.getHeightCm() + " from " + attr.getHeightCM());
            return false;
        }
        if (prefsUser.getWeightKg() != attr.getWeightKG()) {
            LOG.info("user changed to " + prefsUser.getWeightKg() + " from " + attr.getWeightKG());
            return false;
        }
        if (!Integer.valueOf(prefsUser.getSleepDuration()).equals(attr.getSleepGoalHPD())) {
            LOG.info("user sleep goal changed to " + prefsUser.getSleepDuration() + " from " + attr.getSleepGoalHPD());
            return false;
        }
        if (!Integer.valueOf(prefsUser.getStepsGoal()).equals(attr.getStepsGoalSPD())) {
            LOG.info("user steps goal changed to " + prefsUser.getStepsGoal() + " from " + attr.getStepsGoalSPD());
            return false;
        }
        return true;
    }

    private static boolean isEqual(DeviceAttributes attr, GBDevice gbDevice) {
        if (!Objects.equals(attr.getFirmwareVersion1(), gbDevice.getFirmwareVersion())) {
            return false;
        }
        if (!Objects.equals(attr.getFirmwareVersion2(), gbDevice.getFirmwareVersion2())) {
            return false;
        }
        if (!Objects.equals(attr.getVolatileIdentifier(), gbDevice.getVolatileAddress())) {
            return false;
        }
        return true;
    }

    /**
     * Finds the corresponding Device entity for the given GBDevice.
     * @param gbDevice
     * @param session
     * @return the corresponding Device entity, or null if none
     */
    @Nullable
    public static Device findDevice(GBDevice gbDevice, DaoSession session) {
        DeviceDao deviceDao = session.getDeviceDao();
        Query<Device> query = deviceDao.queryBuilder().where(DeviceDao.Properties.Identifier.eq(gbDevice.getAddress())).build();
        List<Device> devices = query.list();
        if (devices.size() > 0) {
            return devices.get(0);
        }
        return null;
    }

    /**
     * Returns all active (that is, not old, archived ones) from the database.
     * (currently the active handling is not available)
     * @param daoSession
     */
    public static List<Device> getActiveDevices(DaoSession daoSession) {
        return daoSession.getDeviceDao().loadAll();
    }

    /**
     * Looks up in the database the Device entity corresponding to the GBDevice. If a device
     * exists already, it will be updated with the current preferences values. If no device exists
     * yet, it will be created in the database.
     *
     * @param session
     * @return the device entity corresponding to the given GBDevice
     */
    public static Device getDevice(GBDevice gbDevice, DaoSession session) {
        Device device = findDevice(gbDevice, session);
        if (device == null) {
            device = createDevice(gbDevice, session);
        } else {
            ensureDeviceUpToDate(device, gbDevice, session);
        }
        if (gbDevice.isInitialized()) {
            ensureDeviceAttributes(device, gbDevice, session);
        }

        return device;
    }

    @NonNull
    public static DeviceAttributes getDeviceAttributes(Device device) {
        List<DeviceAttributes> list = device.getDeviceAttributesList();
        if (list.isEmpty()) {
            throw new IllegalStateException("device has no attributes");
        }
        return list.get(0);
    }

    private static void ensureDeviceUpToDate(Device device, GBDevice gbDevice, DaoSession session) {
        if (!isDeviceUpToDate(device, gbDevice)) {
            device.setIdentifier(gbDevice.getAddress());
            device.setName(gbDevice.getName());
            DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
            device.setManufacturer(coordinator.getManufacturer());
            device.setType(gbDevice.getType().getKey());
            device.setModel(gbDevice.getModel());

            if (device.getId() == null) {
                session.getDeviceDao().insert(device);
            } else {
                session.getDeviceDao().update(device);
            }
        }
    }

    private static boolean isDeviceUpToDate(Device device, GBDevice gbDevice) {
        if (!Objects.equals(device.getIdentifier(), gbDevice.getAddress())) {
            return false;
        }
        if (!Objects.equals(device.getName(), gbDevice.getName())) {
            return false;
        }
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        if (!Objects.equals(device.getManufacturer(), coordinator.getManufacturer())) {
            return false;
        }
        if (device.getType() != gbDevice.getType().getKey()) {
            return false;
        }
        if (!Objects.equals(device.getModel(), gbDevice.getModel())) {
            return false;
        }
        return true;
    }

    private static Device createDevice(GBDevice gbDevice, DaoSession session) {
        Device device = new Device();
        ensureDeviceUpToDate(device, gbDevice, session);

        return device;
    }

    private static void ensureDeviceAttributes(Device device, GBDevice gbDevice, DaoSession session) {
        List<DeviceAttributes> deviceAttributes = device.getDeviceAttributesList();
        DeviceAttributes[] previousDeviceAttributes = new DeviceAttributes[1];
        if (hasUpToDateDeviceAttributes(deviceAttributes, gbDevice, previousDeviceAttributes)) {
            return;
        }

        Calendar now = DateTimeUtils.getCalendarUTC();
        invalidateDeviceAttributes(previousDeviceAttributes[0], now, session);

        DeviceAttributes attributes = new DeviceAttributes();
        attributes.setDeviceId(device.getId());
        attributes.setValidFromUTC(now.getTime());
        attributes.setFirmwareVersion1(gbDevice.getFirmwareVersion());
        attributes.setFirmwareVersion2(gbDevice.getFirmwareVersion2());
        attributes.setVolatileIdentifier(gbDevice.getVolatileAddress());
        DeviceAttributesDao attributesDao = session.getDeviceAttributesDao();
        attributesDao.insert(attributes);

// sort order is important, so we re-fetch from the db
//        deviceAttributes.add(attributes);
        device.resetDeviceAttributesList();
    }

    private static void invalidateDeviceAttributes(DeviceAttributes deviceAttributes, Calendar now, DaoSession session) {
        if (deviceAttributes != null) {
            Calendar invalid = (Calendar) now.clone();
            invalid.add(Calendar.MINUTE, -1);
            deviceAttributes.setValidToUTC(invalid.getTime());
            session.getDeviceAttributesDao().update(deviceAttributes);
        }
    }

    private static boolean hasUpToDateDeviceAttributes(List<DeviceAttributes> deviceAttributes, GBDevice gbDevice, DeviceAttributes[] outPreviousAttributes) {
        for (DeviceAttributes attr : deviceAttributes) {
            if (!isValidNow(attr)) {
                continue;
            }
            if (isEqual(attr, gbDevice)) {
                return true;
            } else {
                outPreviousAttributes[0] = attr;
            }
        }
        return false;
    }

    @NonNull
    public static List<ActivityDescription> findActivityDecriptions(@NonNull User user, int tsFrom, int tsTo, @NonNull DaoSession session) {
        Property tsFromProperty = ActivityDescriptionDao.Properties.TimestampFrom;
        Property tsToProperty = ActivityDescriptionDao.Properties.TimestampTo;
        Property userIdProperty = ActivityDescriptionDao.Properties.UserId;
        QueryBuilder<ActivityDescription> qb = session.getActivityDescriptionDao().queryBuilder();
        qb.where(userIdProperty.eq(user.getId()), isAtLeastPartiallyInRange(qb, tsFromProperty, tsToProperty, tsFrom, tsTo));
        List<ActivityDescription> descriptions = qb.build().list();
        return descriptions;
    }

    /**
     * Returns a condition that matches when the range of the entity (tsFromProperty..tsToProperty)
     * is completely or partially inside the range tsFrom..tsTo.
     * @param qb the query builder to use
     * @param tsFromProperty the property indicating the start of the entity's range
     * @param tsToProperty the property indicating the end of the entity's range
     * @param tsFrom the timestamp indicating the start of the range to match
     * @param tsTo the timestamp indicating the end of the range to match
     * @param <T> the query builder's type parameter
     * @return the range WhereCondition
     */
    private static <T> WhereCondition isAtLeastPartiallyInRange(QueryBuilder<T> qb, Property tsFromProperty, Property tsToProperty, int tsFrom, int tsTo) {
        return qb.and(tsFromProperty.lt(tsTo), tsToProperty.gt(tsFrom));
    }

    @NonNull
    public static ActivityDescription createActivityDescription(@NonNull User user, int tsFrom, int tsTo, @NonNull DaoSession session) {
        ActivityDescription desc = new ActivityDescription();
        desc.setUser(user);
        desc.setTimestampFrom(tsFrom);
        desc.setTimestampTo(tsTo);
        session.getActivityDescriptionDao().insertOrReplace(desc);

        return desc;
    }

    @NonNull
    public static Tag getTag(@NonNull User user, @NonNull String name, @NonNull DaoSession session) {
        TagDao tagDao = session.getTagDao();
        QueryBuilder<Tag> qb = tagDao.queryBuilder();
        Query<Tag> query = qb.where(TagDao.Properties.UserId.eq(user.getId()), TagDao.Properties.Name.eq(name)).build();
        List<Tag> tags = query.list();
        if (tags.size() > 0) {
            return tags.get(0);
        }
        return createTag(user, name, null, session);
    }

    static Tag createTag(@NonNull User user, @NonNull String name, @Nullable String description, @NonNull DaoSession session) {
        Tag tag = new Tag();
        tag.setUserId(user.getId());
        tag.setName(name);
        tag.setDescription(description);
        session.getTagDao().insertOrReplace(tag);
        return tag;
    }

    /**
     * Returns all user-configurable alarms for the given user and device. The list is sorted by
     * {@link Alarm#position}. Calendar events that may also be modeled as alarms are not stored
     * in the database and hence not returned by this method.
     * @param gbDevice the device for which the alarms shall be loaded
     * @return the list of alarms for the given device
     */
    @NonNull
    public static List<Alarm> getAlarms(@NonNull GBDevice gbDevice) {
        DeviceCoordinator coordinator = DeviceHelper.getInstance().getCoordinator(gbDevice);
        Prefs prefs = GBApplication.getPrefs();
        // TODO: this alarm reservation is a device dependent detail
        int reservedSlots = prefs.getInt(MiBandConst.PREF_MIBAND_RESERVE_ALARM_FOR_CALENDAR, 0);
        int alarmSlots = coordinator.getAlarmSlotCount();

        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            User user = getUser(daoSession);
            Device dbDevice = DBHelper.findDevice(gbDevice, daoSession);
            if (dbDevice != null) {
                AlarmDao alarmDao = daoSession.getAlarmDao();
                Long deviceId = dbDevice.getId();
                QueryBuilder<Alarm> qb = alarmDao.queryBuilder();
                qb.where(
                        AlarmDao.Properties.UserId.eq(user.getId()),
                        AlarmDao.Properties.DeviceId.eq(deviceId)).orderAsc(AlarmDao.Properties.Position).limit(alarmSlots - reservedSlots);
                return qb.build().list();
            }
        } catch (Exception e) {
            LOG.warn("Error reading alarms from db", e);
        }
        return Collections.emptyList();
    }

    public static void store(Alarm alarm) {
        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            daoSession.insertOrReplace(alarm);
        } catch (Exception e) {
             LOG.error("Error acquiring database", e);
        }
    }

    public static void clearSession() {
        try (DBHandler dbHandler = GBApplication.acquireDB()) {
            DaoSession session = dbHandler.getDaoSession();
            session.clear();
        } catch (Exception e) {
            LOG.warn("Unable to acquire database to clear the session", e);
        }
    }
}
