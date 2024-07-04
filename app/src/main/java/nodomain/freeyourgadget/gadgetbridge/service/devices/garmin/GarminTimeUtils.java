package nodomain.freeyourgadget.gadgetbridge.service.devices.garmin;

import java.time.Instant;
import java.time.ZoneId;

public class GarminTimeUtils {

    public static final int GARMIN_TIME_EPOCH = 631065600;

    public static int unixTimeToGarminTimestamp(int unixTime) {
        return unixTime - GARMIN_TIME_EPOCH;
    }

    public static int javaMillisToGarminTimestamp(long millis) {
        return (int) (millis / 1000) - GARMIN_TIME_EPOCH;
    }

    public static long garminTimestampToJavaMillis(int timestamp) {
        return (timestamp + GARMIN_TIME_EPOCH) * 1000L;
    }

    public static int garminTimestampToUnixTime(int timestamp) {
        return timestamp + GARMIN_TIME_EPOCH;
    }

    public static int unixTimeToGarminDayOfWeek(int unixTime) {
        return (Instant.ofEpochSecond(unixTime).atZone(ZoneId.systemDefault()).getDayOfWeek().getValue() % 7);
    }
}
