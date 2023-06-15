package nodomain.freeyourgadget.gadgetbridge.service.devices.vivomovehr;

import nodomain.freeyourgadget.gadgetbridge.devices.vivomovehr.VivomoveConstants;

public final class GarminTimeUtils {
    private GarminTimeUtils() {
    }

    public static int unixTimeToGarminTimestamp(int unixTime) {
        return unixTime - VivomoveConstants.GARMIN_TIME_EPOCH;
    }

    public static int javaMillisToGarminTimestamp(long millis) {
        return (int) (millis / 1000) - VivomoveConstants.GARMIN_TIME_EPOCH;
    }

    public static long garminTimestampToJavaMillis(int timestamp) {
        return (timestamp + VivomoveConstants.GARMIN_TIME_EPOCH) * 1000L;
    }

    public static int garminTimestampToUnixTime(int timestamp) {
        return timestamp + VivomoveConstants.GARMIN_TIME_EPOCH;
    }
}
