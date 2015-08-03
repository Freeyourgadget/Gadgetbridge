package nodomain.freeyourgadget.gadgetbridge.model;

import android.os.Parcelable;

import java.util.Calendar;

public interface Alarm extends Parcelable, Comparable<Alarm> {
    public static final byte ALARM_ONCE = 0;
    public static final byte ALARM_MON = 1;
    public static final byte ALARM_TUE = 2;
    public static final byte ALARM_WED = 4;
    public static final byte ALARM_THU = 8;
    public static final byte ALARM_FRI = 16;
    public static final byte ALARM_SAT = 32;
    public static final byte ALARM_SUN = 64;

    int getIndex();

    Calendar getAlarmCal();

    String getTime();

    boolean isEnabled();

    boolean isSmartWakeup();

    int getRepetitionMask();

    boolean getRepetition(int dow);
}
