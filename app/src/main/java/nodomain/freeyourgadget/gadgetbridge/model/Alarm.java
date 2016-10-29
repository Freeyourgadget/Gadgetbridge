package nodomain.freeyourgadget.gadgetbridge.model;

import android.os.Parcelable;

import java.util.Calendar;

public interface Alarm extends Parcelable, Comparable<Alarm> {
    byte ALARM_ONCE = 0;
    byte ALARM_MON = 1;
    byte ALARM_TUE = 2;
    byte ALARM_WED = 4;
    byte ALARM_THU = 8;
    byte ALARM_FRI = 16;
    byte ALARM_SAT = 32;
    byte ALARM_SUN = 64;

    int getIndex();

    Calendar getAlarmCal();

    String getTime();

    boolean isEnabled();

    boolean isSmartWakeup();

    int getRepetitionMask();

    boolean isRepetitive();

    boolean getRepetition(int dow);
}
