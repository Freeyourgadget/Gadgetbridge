package nodomain.freeyourgadget.gadgetbridge.util;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventBatteryInfo;

/**
 * Created by Raziel on 13/04/2017.
 */

public class BatteryNormalize {
    private int counter = 0;
    private int levelMid = 0;
    private GregorianCalendar lastCheck = new GregorianCalendar();
    private static int INTERVAL_SECOND_CHECK  = 30;
    private static int MAX_COUNT_CHECK = 6;

    public void BatteryNormalize(int interval,int numerberCheck){
        INTERVAL_SECOND_CHECK=interval;
        MAX_COUNT_CHECK=numerberCheck;
    }

    //use this function if the battery level of the device oscillates
    public short getLevelNormalize(int data, GBDeviceEventBatteryInfo cmd) {
        if ((Calendar.getInstance().getTimeInMillis()-lastCheck.getTimeInMillis() ) / 1000 >= INTERVAL_SECOND_CHECK) {
            lastCheck.setTimeInMillis(Calendar.getInstance().getTimeInMillis());
            if (data > levelMid || counter == 0) {
                levelMid = data;
            }
            counter += 1;
        }
        if (counter == MAX_COUNT_CHECK) {
            counter = 0;
            return (short) levelMid;
        } else {
            return cmd.level;
        }
    }

    public int getCounter  (){
        return  counter;
    }
}
