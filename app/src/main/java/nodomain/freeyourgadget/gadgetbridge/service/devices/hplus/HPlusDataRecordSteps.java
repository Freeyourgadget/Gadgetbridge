package nodomain.freeyourgadget.gadgetbridge.service.devices.hplus;

/*
* @author João Paulo Barraca &lt;jpbarraca@gmail.com&gt;
*/


import java.util.Calendar;


class HPlusDataRecordSteps extends HPlusDataRecord{
    public int steps;
    public int distance;

    HPlusDataRecordSteps(byte[] data) {
        super(data);

        int year =  (data[10] & 0xFF) * 256 + (data[9] & 0xFF);
        int month = data[11] & 0xFF;
        int day = data[12] & 0xFF;

        if (year < 2000 || month > 12 || day > 31) {
            throw new IllegalArgumentException("Invalid record date "+year+"-"+month+"-"+day);
        }
        steps = (data[2] & 0xFF) * 256 + (data[1] & 0xFF);
        distance = (data[4] & 0xFF) * 256 + (data[3] & 0xFF);

            /*
            unknown fields
            short s12 = (short)(data[5] + data[6] * 256);
            short s13 = (short)(data[7] + data[8] * 256);
            short s16 = (short)(data[13]) + data[14] * 256);
            short s17 = data[15];
            short s18 = data[16];
            */

        Calendar date = Calendar.getInstance();
        date.set(Calendar.YEAR, year);
        date.set(Calendar.MONTH, month - 1);
        date.set(Calendar.DAY_OF_MONTH, day);
        date.set(Calendar.HOUR_OF_DAY, 23);
        date.set(Calendar.MINUTE, 59);
        date.set(Calendar.SECOND, 59);
        date.set(Calendar.MILLISECOND, 999);

        timestamp = (int) (date.getTimeInMillis() / 1000);
    }
}