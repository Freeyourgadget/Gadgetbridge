package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import java.util.Calendar;
import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;

public class BatteryInfo extends AbstractInfo {
    public BatteryInfo(byte[] data) {
        super(data);
    }

    public int getLevelInPercent() {
        if (mData.length >= 1) {
            return mData[0];
        }
        return 50; // actually unknown
    }

    public String getStatus() {
        if (mData.length >= 10) {
            int value = mData[9];
            switch (value) {
                case 1:
                    return GBApplication.getContext().getString(R.string.battery_low);
                case 2:
                    return GBApplication.getContext().getString(R.string.battery_medium);
                case 3:
                    return GBApplication.getContext().getString(R.string.battery_full);
                case 4:
                    return GBApplication.getContext().getString(R.string.battery_not_charging);
            }
        }
        return GBApplication.getContext().getString(R.string._unknown_);
    }

    public GregorianCalendar getLastChargeTime() {
        GregorianCalendar lastCharge = new GregorianCalendar();

        if (mData.length >= 10) {
            lastCharge.set(Calendar.YEAR, (2000 + mData[1]));
            lastCharge.set(Calendar.MONTH, mData[2]);
            lastCharge.set(Calendar.DATE, mData[3]);
            lastCharge.set(Calendar.HOUR_OF_DAY, mData[4]);
            lastCharge.set(Calendar.MINUTE, mData[5]);
            lastCharge.set(Calendar.SECOND, mData[6]);
        }

        return lastCharge;
    }

    public int getNumCharges() {
        if (mData.length >= 10) {
            return ((0xff & mData[7]) | ((0xff & mData[8]) << 8));

        }
        return -1;
    }
}
