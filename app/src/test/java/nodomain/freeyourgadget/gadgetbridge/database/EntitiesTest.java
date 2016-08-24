package nodomain.freeyourgadget.gadgetbridge.database;

import android.database.sqlite.SQLiteDatabase;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.BuildConfig;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoMaster;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.entities.User;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributes;
import nodomain.freeyourgadget.gadgetbridge.entities.UserAttributesDao;
import nodomain.freeyourgadget.gadgetbridge.entities.UserDao;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityUser;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 19) // need sdk 19 because "WITHOUT ROWID" is not supported in robolectric/sqlite4java
public class EntitiesTest {

    private DaoSession daoSession;
    private UserDao userDao;
    private UserAttributesDao userAttributesDao;

    @Before
    public void setUp() {
        DaoMaster.DevOpenHelper openHelper = new DaoMaster.DevOpenHelper(RuntimeEnvironment.application, null, null);
        SQLiteDatabase db = openHelper.getWritableDatabase();
        daoSession = new DaoMaster(db).newSession();
        userDao = daoSession.getUserDao();
        userAttributesDao = daoSession.getUserAttributesDao();
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


}
