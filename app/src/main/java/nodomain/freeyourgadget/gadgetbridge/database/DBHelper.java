package nodomain.freeyourgadget.gadgetbridge.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Toast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.greenrobot.dao.Property;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;
import de.greenrobot.dao.query.WhereCondition;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.devices.AbstractSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.DeviceCoordinator;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleHealthSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.devices.pebble.PebbleMisfitSampleProvider;
import nodomain.freeyourgadget.gadgetbridge.entities.AbstractActivitySample;
import nodomain.freeyourgadget.gadgetbridge.entities.ActivityDescription;
import nodomain.freeyourgadget.gadgetbridge.entities.ActivityDescriptionDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceDao;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlay;
import nodomain.freeyourgadget.gadgetbridge.entities.PebbleHealthActivityOverlayDao;
import nodomain.freeyourgadget.gadgetbridge.entities.Tag;
import nodomain.freeyourgadget.gadgetbridge.entities.TagDao;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.UserDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityKind;
import nodomain.freeyourgadget.gadgetbridge.model.ActivitySample;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.ValidByDate;
import nodomain.freeyourgadget.gadgetbridge.service.devices.pebble.PebbleProtocol;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DeviceHelper;
import nodomain.freeyourgadget.gadgetbridge.util.FileUtils;
import nodomain.freeyourgadget.gadgetbridge.util.GB;

import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_CUSTOM_SHORT;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_INTENSITY;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_STEPS;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TIMESTAMP;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.KEY_TYPE;
import static nodomain.freeyourgadget.gadgetbridge.database.DBConstants.TABLE_GBACTIVITYSAMPLES;

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
        return true;
    }

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
        ensureDeviceAttributes(device, gbDevice, session);

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
     * Returns the old activity database handler if there is any content in that
     * db, or null otherwise.
     *
     * @return the old activity db handler or null
     */
    @Nullable
    public ActivityDatabaseHandler getOldActivityDatabaseHandler() {
        ActivityDatabaseHandler handler = new ActivityDatabaseHandler(context);
        if (handler.hasContent()) {
            return handler;
        }
        return null;
    }

    public void importOldDb(ActivityDatabaseHandler oldDb, GBDevice targetDevice, DBHandler targetDBHandler) {
        DaoSession tempSession = targetDBHandler.getDaoMaster().newSession();
        try {
            importActivityDatabase(oldDb, targetDevice, tempSession);
        } finally {
            tempSession.clear();
        }
    }

    private boolean isEmpty(DaoSession session) {
        long totalSamplesCount = session.getMiBandActivitySampleDao().count();
        totalSamplesCount += session.getPebbleHealthActivitySampleDao().count();
        return totalSamplesCount == 0;
    }

    private void importActivityDatabase(ActivityDatabaseHandler oldDbHandler, GBDevice targetDevice, DaoSession session) {
        try (SQLiteDatabase oldDB = oldDbHandler.getReadableDatabase()) {
            User user = DBHelper.getUser(session);
            for (DeviceCoordinator coordinator : DeviceHelper.getInstance().getAllCoordinators()) {
                if (coordinator.supports(targetDevice)) {
                    AbstractSampleProvider<? extends AbstractActivitySample> sampleProvider = (AbstractSampleProvider<? extends AbstractActivitySample>) coordinator.getSampleProvider(targetDevice, session);
                    importActivitySamples(oldDB, targetDevice, session, sampleProvider, user);
                    break;
                }
            }
        }
    }

    private <T extends AbstractActivitySample> void importActivitySamples(SQLiteDatabase fromDb, GBDevice targetDevice, DaoSession targetSession, AbstractSampleProvider<T> sampleProvider, User user) {
        if (sampleProvider instanceof PebbleMisfitSampleProvider) {
            GB.toast(context, "Migration of old Misfit data is not supported!", Toast.LENGTH_LONG, GB.WARN);
            return;
        }

        String order = "timestamp";
        final String where = "provider=" + sampleProvider.getID();

        boolean convertActivityTypeToRange = false;
        int currentTypeRun, previousTypeRun, currentTimeStamp, currentTypeStartTimeStamp, currentTypeEndTimeStamp;
        List<PebbleHealthActivityOverlay> overlayList = new ArrayList<>();

        final int BATCH_SIZE = 100000; // 100.000 samples = rougly 20 MB per batch
        List<T> newSamples;
        if (sampleProvider instanceof PebbleHealthSampleProvider) {
            convertActivityTypeToRange = true;
            previousTypeRun = ActivitySample.NOT_MEASURED;
            currentTypeStartTimeStamp = -1;
            currentTypeEndTimeStamp = -1;

        } else {
            previousTypeRun = currentTypeStartTimeStamp = currentTypeEndTimeStamp = 0;
        }
        try (Cursor cursor = fromDb.query(TABLE_GBACTIVITYSAMPLES, null, where, null, null, null, order)) {
            int colTimeStamp = cursor.getColumnIndex(KEY_TIMESTAMP);
            int colIntensity = cursor.getColumnIndex(KEY_INTENSITY);
            int colSteps = cursor.getColumnIndex(KEY_STEPS);
            int colType = cursor.getColumnIndex(KEY_TYPE);
            int colCustomShort = cursor.getColumnIndex(KEY_CUSTOM_SHORT);
            long deviceId = DBHelper.getDevice(targetDevice, targetSession).getId();
            long userId = user.getId();
            newSamples = new ArrayList<>(Math.min(BATCH_SIZE, cursor.getCount()));
            while (cursor.moveToNext()) {
                T newSample = sampleProvider.createActivitySample();
                newSample.setProvider(sampleProvider);
                newSample.setUserId(userId);
                newSample.setDeviceId(deviceId);
                currentTimeStamp = cursor.getInt(colTimeStamp);
                newSample.setTimestamp(currentTimeStamp);
                newSample.setRawIntensity(getNullableInt(cursor, colIntensity, ActivitySample.NOT_MEASURED));
                currentTypeRun = getNullableInt(cursor, colType, ActivitySample.NOT_MEASURED);
                newSample.setRawKind(currentTypeRun);
                if (convertActivityTypeToRange) {
                    //at the beginning there is no start timestamp
                    if (currentTypeStartTimeStamp == -1) {
                        currentTypeStartTimeStamp = currentTypeEndTimeStamp = currentTimeStamp;
                        previousTypeRun = currentTypeRun;
                    }

                    if (currentTypeRun != previousTypeRun) {
                        //we used not to store the last sample, now we do the opposite and we need to round up
                        currentTypeEndTimeStamp = currentTimeStamp;
                        //if the Type has changed, the run has ended. Only store light and deep sleep data
                        if (previousTypeRun == 4) {
                            overlayList.add(new PebbleHealthActivityOverlay(currentTypeStartTimeStamp, currentTypeEndTimeStamp, sampleProvider.toRawActivityKind(ActivityKind.TYPE_LIGHT_SLEEP), deviceId, userId, null));
                        } else if (previousTypeRun == 5) {
                            overlayList.add(new PebbleHealthActivityOverlay(currentTypeStartTimeStamp, currentTypeEndTimeStamp, sampleProvider.toRawActivityKind(ActivityKind.TYPE_DEEP_SLEEP), deviceId, userId, null));
                        }
                        currentTypeStartTimeStamp = currentTimeStamp;
                        previousTypeRun = currentTypeRun;
                    } else {
                        //just expand the run
                        currentTypeEndTimeStamp = currentTimeStamp;
                    }

                }
                newSample.setSteps(getNullableInt(cursor, colSteps, ActivitySample.NOT_MEASURED));
                if (colCustomShort > -1) {
                    newSample.setHeartRate(getNullableInt(cursor, colCustomShort, ActivitySample.NOT_MEASURED));
                } else {
                    newSample.setHeartRate(ActivitySample.NOT_MEASURED);
                }
                newSamples.add(newSample);

                if ((newSamples.size() % BATCH_SIZE) == 0) {
                    sampleProvider.getSampleDao().insertOrReplaceInTx(newSamples, true);
                    targetSession.clear();
                    newSamples.clear();
                }
            }
            // and insert the remaining samples
            if (!newSamples.isEmpty()) {
                sampleProvider.getSampleDao().insertOrReplaceInTx(newSamples, true);
            }
            // store the overlay records
            if (!overlayList.isEmpty()) {
                PebbleHealthActivityOverlayDao overlayDao = targetSession.getPebbleHealthActivityOverlayDao();
                overlayDao.insertOrReplaceInTx(overlayList);
            }
        }
    }

    private int getNullableInt(Cursor cursor, int columnIndex, int defaultValue) {
        if (cursor.isNull(columnIndex)) {
            return defaultValue;
        }
        return cursor.getInt(columnIndex);
    }
}
