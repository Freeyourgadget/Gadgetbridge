package nodomain.freeyourgadget.gadgetbridge.util;

import java.text.ParseException;
import java.util.Date;

public class GBPrefs {

    public static final String AUTO_RECONNECT = "general_autocreconnect";
    public static boolean AUTO_RECONNECT_DEFAULT = true;

    public static final String USER_NAME = "mi_user_alias";
    public static final String USER_NAME_DEFAULT = "gadgetbridge-user";
    private static final String USER_BIRTHDAY = "";

    private final Prefs mPrefs;

    public GBPrefs(Prefs prefs) {
        mPrefs = prefs;
    }

    public boolean getAutoReconnect() {
        return mPrefs.getBoolean(AUTO_RECONNECT, AUTO_RECONNECT_DEFAULT);
    }

    public String getUserName() {
        return mPrefs.getString(USER_NAME, USER_NAME_DEFAULT);
    }

    public Date getUserBirthday() {
        String date = mPrefs.getString(USER_BIRTHDAY, null);
        if (date == null) {
            return null;
        }
        try {
            return DateTimeUtils.dayFromString(date);
        } catch (ParseException ex) {
            GB.log("Error parsing date: " + date, GB.ERROR, ex);
            return null;
        }
    }

    public int getUserSex() {
        return 0;
    }
}
