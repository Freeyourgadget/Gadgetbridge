package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class SonySWR12Util {

    public static long secSince2013() {
        //sony uses time on band since 2013 for some reason
        final Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        instance.set(2013, 0, 1, 0, 0, 0);
        instance.set(14, 0);
        return instance.getTimeInMillis()/1000;
    }

    public static String timeToString(long sec) {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss");
        return format.format(new Date(sec * 1000));
    }
}
