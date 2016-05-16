package nodomain.freeyourgadget.gadgetbridge.util;

public class GBPrefs {

    public static final String AUTO_RECONNECT = "general_autocreconnect";
    public static boolean AUTO_RECONNECT_DEFAULT = true;
    private final Prefs mPrefs;

    public GBPrefs(Prefs prefs) {
        mPrefs = prefs;
    }

    public boolean getAutoReconnect() {
        return mPrefs.getBoolean(AUTO_RECONNECT, AUTO_RECONNECT_DEFAULT);
    }
}
