package nodomain.freeyourgadget.gadgetbridge;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Calendar;

import static nodomain.freeyourgadget.gadgetbridge.miband.MiBandConst.PREF_MIBAND_ALARM_PREFIX;

public class GBAlarm {
    private int index;
    private boolean enabled;
    private boolean smartWakeup;
    private int repetition;
    private int hour;
    private int minute;

    public static final byte ALARM_ONCE = 0;
    public static final byte ALARM_MON = 1;
    public static final byte ALARM_TUE = 2;
    public static final byte ALARM_WED = 4;
    public static final byte ALARM_THU = 8;
    public static final byte ALARM_FRI = 16;
    public static final byte ALARM_SAT = 32;
    public static final byte ALARM_SUN = 64;

    public static final String DEFAULT_ALARM1 = "0,false,true,31,7,30";
    public static final String DEFAULT_ALARM2 = "1,false,false,96,8,00";
    public static final String DEFAULT_ALARM3 = "2,false,false,0,15,30";

    public GBAlarm(int index, boolean enabled, boolean smartWakeup, byte repetition, int hour, int minute) {
        this.index = index;
        this.enabled = enabled;
        this.smartWakeup = smartWakeup;
        this.repetition = repetition;
        this.hour = hour;
        this.minute = minute;
        store();
    }

    public GBAlarm(String fromPreferences){
        String[] tokens = fromPreferences.split(",");
        //TODO: sanify the string!
        this.index = Integer.parseInt(tokens[0]);
        this.enabled = Boolean.parseBoolean(tokens[1]);
        this.smartWakeup = Boolean.parseBoolean(tokens[2]);
        this.repetition =  Integer.parseInt(tokens[3]);
        this.hour = Integer.parseInt(tokens[4]);
        this.minute = Integer.parseInt(tokens[5]);
        store();
    }

    public int getIndex() {
        return this.index;
    }

    public String getTime() {
        return String.format("%02d",this.hour) + ":" + String.format("%02d",this.minute);
    }

    public int getHour(){
        return this.hour;
    }

    public int getMinute(){
        return this.minute;
    }
    public Calendar getAlarmCal() {
        Calendar alarm = Calendar.getInstance();
        alarm.set(Calendar.HOUR_OF_DAY, this.hour);
        alarm.set(Calendar.MINUTE, this.minute);
        return alarm;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isSmartWakeup() {
        return this.smartWakeup;
    }

    public boolean getRepetition(int dow) {
        return (this.repetition & dow) > 0;
    }

    public int getRepetitionMask() {
        return this.repetition;
    }

    public String toPreferences() {
        return  String.valueOf(this.index)+','+
                String.valueOf(this.enabled)+','+
                String.valueOf(this.smartWakeup)+','+
                String.valueOf(this.repetition)+','+
                String.valueOf(this.hour)+','+
                String.valueOf(this.minute);
    }

    public void setSmartWakeup(boolean smartWakeup) {
        this.smartWakeup = smartWakeup;
        store();
    }

    public void setRepetition(boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat, boolean sun) {
        this.repetition = ALARM_ONCE |
                (mon ? ALARM_MON : 0) |
                (tue ? ALARM_TUE : 0) |
                (wed ? ALARM_WED : 0) |
                (thu ? ALARM_THU : 0) |
                (fri ? ALARM_FRI : 0) |
                (sat ? ALARM_SAT : 0) |
                (sun ? ALARM_SUN : 0);
        store();
    }

    public void setHour(int hour) {
        this.hour = hour;
        store();
    }

    public void setMinute(int minute) {
        this.minute = minute;
        store();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        store(); // TODO: if we have many setters, this may become a bottleneck
    }

    private void store() {
        //TODO: I don't like to have the alarm index both in the preference name and in the value
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(GBApplication.getContext());
        String pref = PREF_MIBAND_ALARM_PREFIX +(this.index+1);
        sharedPrefs.edit().putString(pref, this.toPreferences()).apply();
    }
}
