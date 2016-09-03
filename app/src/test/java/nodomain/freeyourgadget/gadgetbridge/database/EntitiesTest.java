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
import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.ActivityDescription;
import nodomain.freeyourgadget.gadgetbridge.entities.ActivityDescriptionDao;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.Device;
import nodomain.freeyourgadget.gadgetbridge.entities.DeviceAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.Tag;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.UserDao;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;
import nodomain.freeyourgadget.gadgetbridge.model.DeviceType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 19)
// need sdk 19 because "WITHOUT ROWID" is not supported in robolectric/sqlite4java
public class EntitiesTest {

    private DaoSession daoSession;
    private UserDao userDao;
    private UserAttributesDao userAttributesDao;
    private DBHandler dbHandler;
    private GBApplication app = (GBApplication) RuntimeEnvironment.application;

    @Before
    public void setUp() throws GBException {
// doesn't work with Robolectric yet
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

        daoSession.getUserDao().insert(user);
        assertNotNull(user.getId());

        attributes.setUserId(user.getId());
        daoSession.getUserAttributesDao().insert(attributes);
        user.getUserAttributesList().add(attributes);

        assertNotNull(userDao.load(user.getId()));
        assertEquals(1, userDao.count());
        assertEquals(1, daoSession.loadAll(User.class).size());

        assertNotNull(userAttributesDao.load(attributes.getId()));
        assertEquals(1, userAttributesDao.count());
        assertEquals(1, daoSession.loadAll(UserAttributes.class).size());

        daoSession.getUserDao().update(user);
        daoSession.delete(user);
        daoSession.delete(attributes);
        daoSession.delete(attributes);
        assertNull(userDao.load(user.getId()));
    }

    @Test
    public void testDBHelper() {
        GBDevice dummyGBDevice = createDummyGDevice("00:00:00:00:01");
        Device device = DBHelper.getDevice(dummyGBDevice, daoSession);
        assertNotNull(device);
        assertEquals("00:00:00:00:01", device.getIdentifier());
        assertEquals("Testie", device.getName());
        assertEquals("4.0", device.getModel());
        assertEquals(DeviceType.TEST.getKey(), device.getType());
        DeviceAttributes attributes = device.getDeviceAttributesList().get(0);
        assertNotNull(attributes);
        assertEquals("1.2.3", attributes.getFirmwareVersion1());
    }

    private GBDevice createDummyGDevice(String macAddress) {
        GBDevice dummyGBDevice = new GBDevice(macAddress, "Testie", DeviceType.TEST);
        dummyGBDevice.setFirmwareVersion("1.2.3");
        dummyGBDevice.setModel("4.0");
        return dummyGBDevice;
    }

    @Test
    public void testActivityDescription() {
        User user = DBHelper.getUser(daoSession);
        assertNotNull(user);

        ActivityDescriptionDao descDao = daoSession.getActivityDescriptionDao();
        assertEquals(0, descDao.count());

        List<ActivityDescription> list = DBHelper.findActivityDecriptions(user, 10, 100, daoSession);
        assertTrue(list.isEmpty());

        ActivityDescription desc = DBHelper.createActivityDescription(user, 10, 100, daoSession);
        assertNotNull(desc);
        assertEquals(user, desc.getUser());
        assertEquals(10, desc.getTimestampFrom());
        assertEquals(100, desc.getTimestampTo());
        List<Tag> tagList = desc.getTagList();
        assertEquals(0, tagList.size());

        Tag t1 = DBHelper.getTag(user, "Table Tennis", daoSession);
        assertNotNull(t1);
        assertEquals("Table Tennis", t1.getName());
        t1.setDescription("Table tennis training for Olympia");
        tagList.add(t1);

        list = DBHelper.findActivityDecriptions(user, 10, 100, daoSession);
        assertEquals(1, list.size());
        ActivityDescription desc1 = list.get(0);
        assertEquals(desc, desc1);
        assertEquals(1, desc1.getTagList().size());

        // check for partial range overlaps
        list = DBHelper.findActivityDecriptions(user, 20, 80, daoSession);
        assertEquals(1, list.size());

        list = DBHelper.findActivityDecriptions(user, 5, 120, daoSession);
        assertEquals(1, list.size());

        list = DBHelper.findActivityDecriptions(user, 20, 120, daoSession);
        assertEquals(1, list.size());

        list = DBHelper.findActivityDecriptions(user, 5, 80, daoSession);
        assertEquals(1, list.size());

        // Now with a second, adjacent ActivityDescription
        ActivityDescription desc2 = DBHelper.createActivityDescription(user, 101, 200, daoSession);

        list = DBHelper.findActivityDecriptions(user, 10, 100, daoSession);
        assertEquals(1, list.size());

        list = DBHelper.findActivityDecriptions(user, 20, 80, daoSession);
        assertEquals(1, list.size());

        list = DBHelper.findActivityDecriptions(user, 5, 120, daoSession);
        assertEquals(2, list.size());

        list = DBHelper.findActivityDecriptions(user, 20, 120, daoSession);
        assertEquals(2, list.size());

        list = DBHelper.findActivityDecriptions(user, 5, 80, daoSession);
        assertEquals(1, list.size());

        // Now with a third, partially overlapping ActivityDescription
        ActivityDescription desc3 = DBHelper.createActivityDescription(user, 5, 15, daoSession);

        list = DBHelper.findActivityDecriptions(user, 10, 100, daoSession);
        assertEquals(2, list.size());

        list = DBHelper.findActivityDecriptions(user, 20, 80, daoSession);
        assertEquals(1, list.size());

        list = DBHelper.findActivityDecriptions(user, 5, 120, daoSession);
        assertEquals(3, list.size());

        list = DBHelper.findActivityDecriptions(user, 20, 120, daoSession);
        assertEquals(2, list.size());

        list = DBHelper.findActivityDecriptions(user, 5, 80, daoSession);
        assertEquals(2, list.size());
    }

    @Test
    public void testDeviceAttributes() throws Exception {
        GBDevice dummyGBDevice = createDummyGDevice("00:00:00:00:02");
        dummyGBDevice.setFirmwareVersion("1.0");
        Device deviceOld = DBHelper.getDevice(dummyGBDevice, daoSession);
        assertNotNull(deviceOld);

        List<DeviceAttributes> attrListOld = deviceOld.getDeviceAttributesList();
        assertEquals(1, attrListOld.size());
        assertEquals("1.0", attrListOld.get(0).getFirmwareVersion1());
        assertEquals("1.0", DBHelper.getDeviceAttributes(deviceOld).getFirmwareVersion1());

        // some time passes, firmware update occurs
        Thread.sleep(2 * 1000);
        dummyGBDevice.setFirmwareVersion("2.0");

        Device deviceNew = DBHelper.getDevice(dummyGBDevice, daoSession);
        assertNotNull(deviceNew);
        List<DeviceAttributes> attrListNew = deviceNew.getDeviceAttributesList();
        assertEquals(2, attrListNew.size());
        assertEquals("2.0", attrListNew.get(0).getFirmwareVersion1());
        assertEquals("1.0", attrListNew.get(1).getFirmwareVersion1());

        assertEquals("2.0", DBHelper.getDeviceAttributes(deviceNew).getFirmwareVersion1());
    }
}
