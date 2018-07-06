package nodomain.freeyourgadget.gadgetbridge.contentprovider;

import android.net.Uri;

public final class HRContentProviderContract {
    public static final String[] deviceColumnNames = new String[]{"Name", "Model", "Address"};
    public static final String[] activityColumnNames = new String[]{"Status", "Message"};
    public static final String[] realtimeColumnNames = new String[]{"Status", "Heartrate", "Steps", "Battery"};

    static final String AUTHORITY = "com.gadgetbridge.heartrate.provider";

    static final String ACTIVITY_START_URL = "content://" + AUTHORITY + "/activity_start";
    static final String ACTIVITY_STOP_URL = "content://" + AUTHORITY + "/activity_stop";
    static final String REALTIME_URL = "content://" + AUTHORITY + "/realtime";
    static final String DEVICES_URL = "content://" + AUTHORITY + "/devices";

    public static final Uri ACTIVITY_START_URI = Uri.parse(ACTIVITY_START_URL);
    public static final Uri ACTIVITY_STOP_URI = Uri.parse(ACTIVITY_STOP_URL);
    public static final Uri REALTIME_URI = Uri.parse(REALTIME_URL);
    public static final Uri DEVICES_URI = Uri.parse(DEVICES_URL);
}
