package nodomain.freeyourgadget.gadgetbridge.impl;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

import static nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandConst.PREF_MIBAND_ALARMS;


public class GBAlarm implements Alarm {

    private final int index;
    private boolean enabled;
    private boolean smartWakeup;
    private int repetition;
    private int hour;
    private int minute;

    public static final String[] DEFAULT_ALARMS = {"2,false,false,0,15,30", "1,false,false,96,8,0", "0,false,true,31,7,30"};


    public GBAlarm(int index, boolean enabled, boolean smartWakeup, byte repetition, int hour, int minute) {
        this.index = index;
        this.enabled = enabled;
        this.smartWakeup = smartWakeup;
        this.repetition = repetition;
        this.hour = hour;
        this.minute = minute;
    }

    public GBAlarm(String fromPreferences) {
        String[] tokens = fromPreferences.split(",");
        //TODO: sanify the string!
        this.index = Integer.parseInt(tokens[0]);
        this.enabled = Boolean.parseBoolean(tokens[1]);
        this.smartWakeup = Boolean.parseBoolean(tokens[2]);
        this.repetition = Integer.parseInt(tokens[3]);
        this.hour = Integer.parseInt(tokens[4]);
        this.minute = Integer.parseInt(tokens[5]);
    }

    private static GBAlarm readFromParcel(Parcel pc) {
        int index = pc.readInt();
        boolean enabled = Boolean.parseBoolean(pc.readString());
        boolean smartWakeup = Boolean.parseBoolean(pc.readString());
        int repetition = pc.readInt();
        int hour = pc.readInt();
        int minute = pc.readInt();
        return new GBAlarm(index, enabled, smartWakeup, (byte) repetition, hour, minute);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GBAlarm) {
            GBAlarm comp = (GBAlarm) o;
            return comp.getIndex() == getIndex();
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getIndex();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.index);
        dest.writeString(String.valueOf(this.enabled));
        dest.writeString(String.valueOf(this.smartWakeup));
        dest.writeInt(this.repetition);
        dest.writeInt(this.hour);
        dest.writeInt(this.minute);
    }

    @Override
    public int compareTo(@NonNull Alarm another) {
        if (this.getIndex() < another.getIndex()) {
            return -1;
        } else if (this.getIndex() > another.getIndex()) {
            return 1;
        }
        return 0;
    }

    @Override
    public int getIndex() {
        return this.index;
    }

    @Override
    public String getTime() {
        return String.format("%02d", this.hour) + ":" + String.format("%02d", this.minute);
    }

    public int getHour() {
        return this.hour;
    }

    public int getMinute() {
        return this.minute;
    }

    @Override
    public Calendar getAlarmCal() {

        Calendar alarm = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        alarm.set(Calendar.HOUR_OF_DAY, this.hour);
        alarm.set(Calendar.MINUTE, this.minute);
        if (now.after(alarm) && repetition == ALARM_ONCE) {
            //if the alarm is in the past set it to tomorrow
            alarm.add(Calendar.DATE, 1);
        }
        return alarm;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public boolean isSmartWakeup() {
        return this.smartWakeup;
    }

    @Override
    public boolean getRepetition(int dow) {
        return (this.repetition & dow) > 0;
    }

    @Override
    public int getRepetitionMask() {
        return this.repetition;
    }

    public String toPreferences() {
        return String.valueOf(this.index) + ',' +
                String.valueOf(this.enabled) + ',' +
                String.valueOf(this.smartWakeup) + ',' +
                String.valueOf(this.repetition) + ',' +
                String.valueOf(this.hour) + ',' +
                String.valueOf(this.minute);
    }

    public void setSmartWakeup(boolean smartWakeup) {
        this.smartWakeup = smartWakeup;
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
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void store() {
        Prefs prefs = GBApplication.getPrefs();
        Set<String> preferencesAlarmListSet = prefs.getStringSet(PREF_MIBAND_ALARMS, new HashSet<String>());
        //the old Set cannot be updated in place see http://developer.android.com/reference/android/content/SharedPreferences.html#getStringSet%28java.lang.String,%20java.util.Set%3Cjava.lang.String%3E%29
        Set<String> newPrefs = new HashSet<>(preferencesAlarmListSet);

        Iterator<String> iterator = newPrefs.iterator();

        while (iterator.hasNext()) {
            String alarmString = iterator.next();
            if (this.equals(new GBAlarm(alarmString))) {
                iterator.remove();
                break;
            }
        }
        newPrefs.add(this.toPreferences());
        prefs.getPreferences().edit().putStringSet(PREF_MIBAND_ALARMS, newPrefs).apply();
    }

    public static final Creator CREATOR = new Creator() {
        public GBAlarm createFromParcel(Parcel in) {
            return readFromParcel(in);
        }

        public GBAlarm[] newArray(int size) {
            return new GBAlarm[size];
        }

    };
}
