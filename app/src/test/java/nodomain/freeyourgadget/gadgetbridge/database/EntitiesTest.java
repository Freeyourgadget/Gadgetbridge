package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.UserDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 19) // need sdk 19 because "WITHOUT ROWID" is not supported in robolectric/sqlite4java
public class EntitiesTest {

    private DaoSession daoSession;
    private UserDao userDao;
    private UserAttributesDao userAttributesDao;
    private DBHandler dbHandler;
    private GBApplication app = (GBApplication) RuntimeEnvironment.application;

    @Before
    public void setUp() throws GBException {
//        dbHandler = GBApplication.acquireDB();
//        daoSession = dbHandler.getDaoSession();
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(app, null, null);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        daoSession = new DaoMaster(db).newSession();
        userDao = daoSession.getUserDao();
        userAttributesDao = daoSession.getUserAttributesDao();
    }

    @After
    public void tearDown() {
//        GBApplication.releaseDB();
    }


    @Test
    public void testUser() {
        User user = new User();
        user.setName("Peter");
        user.setGender(ActivityUser.GENDER_FEMALE);
        Calendar cal = GregorianCalendar.getInstance();
        cal.add(Calendar.YEAR, -20);
        user.setBirthday(cal.getTime());
        UserAttributes attributes = new UserAttributes();
        attributes.setWeightKG(55);
        attributes.setHeightCM(170);
        attributes.setSleepGoalHPD(8);
        attributes.setStepsGoalSPD(10000);

        daoSession.insert(user);
        assertNotNull(user.getId());

        attributes.setUserId(user.getId());
        daoSession.insert(attributes);
        user.getUserAttributesList().add(attributes);

        assertNotNull(userDao.load(user.getId()));
        assertEquals(1, userDao.count());
        assertEquals(1, daoSession.loadAll(User.class).size());

        assertNotNull(userAttributesDao.load(attributes.getId()));
        assertEquals(1, userAttributesDao.count());
        assertEquals(1, daoSession.loadAll(UserAttributes.class).size());

        daoSession.update(user);
        daoSession.delete(user);
        daoSession.delete(attributes);
        daoSession.delete(attributes);
        assertNull(userDao.load(user.getId()));
    }

    @Test
    public void testDBHelper() {
//        DBHelper dbHelper = new DBHelper(RuntimeEnvironment.application);
        GBDevice dummyGBDevice = new GBDevice("00:00:00:00:00", "Testie", DeviceType.TEST);
        dummyGBDevice.setFirmwareVersion("1.2.3");
        dummyGBDevice.setModel("4.0");
        Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
        assertNotNull(device);
        assertEquals("00:00:00:00:00", device.getIdentifier());
        assertEquals("Testie", device.getName());
//        assertEquals("4.0", device.get());
        assertEquals(DeviceType.TEST.getKey(), device.getType());
        DeviceAttributes attributes = device.getDeviceAttributesList().get(0);
        assertNotNull(attributes);
        assertEquals("1.2.3", attributes.getFirmwareVersion1());
    }

}
