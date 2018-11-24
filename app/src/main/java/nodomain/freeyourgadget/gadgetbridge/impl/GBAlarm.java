/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.impl;

import android.os.Parcel;
import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.database.DBHandler;
import nodomain.freeyourgadget.gadgetbridge.database.DBHelper;
import nodomain.freeyourgadget.gadgetbridge.entities.DaoSession;
import nodomain.freeyourgadget.gadgetbridge.model.Alarm;


public class GBAlarm implements Alarm {

    private final int index;
    private boolean enabled;
    private boolean smartWakeup;
    private int repetition;
    private int hour;
    private int minute;
    private long deviceId;

    public GBAlarm(Long deviceId, int index, boolean enabled, boolean smartWakeup, int repetition, int hour, int minute) {
        this.deviceId = deviceId;
        this.index = index;
        this.enabled = enabled;
        this.smartWakeup = smartWakeup;
        this.repetition = repetition;
        this.hour = hour;
        this.minute = minute;
    }

    public static GBAlarm createSingleShot(long deviceId, int index, boolean smartWakeup, Calendar calendar) {
        return new GBAlarm(deviceId, index, true, smartWakeup, Alarm.ALARM_ONCE, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    private static GBAlarm readFromParcel(Parcel pc) {
        long deviceId = pc.readLong();
        int index = pc.readInt();
        boolean enabled = Boolean.parseBoolean(pc.readString());
        boolean smartWakeup = Boolean.parseBoolean(pc.readString());
        int repetition = pc.readInt();
        int hour = pc.readInt();
        int minute = pc.readInt();
        return new GBAlarm(deviceId, index, enabled, smartWakeup, repetition, hour, minute);
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
        dest.writeLong(deviceId);
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
        return String.format(Locale.US, "%02d", this.hour) + ":" + String.format(Locale.US, "%02d", this.minute);
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

    @Override
    public boolean isRepetitive() {
        return getRepetitionMask() != ALARM_ONCE;
    }

    public void setSmartWakeup(boolean smartWakeup) {
        this.smartWakeup = smartWakeup;
    }

    public void setRepetition(boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat, boolean sun) {
        this.repetition = (mon ? ALARM_MON : 0) |
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
        try (DBHandler db = GBApplication.acquireDB()) {
            DaoSession daoSession = db.getDaoSession();
            Long userId = DBHelper.getUser(daoSession).getId();
            nodomain.freeyourgadget.gadgetbridge.entities.Alarm alarm = new nodomain.freeyourgadget.gadgetbridge.entities.Alarm(deviceId, userId, index, enabled, smartWakeup, repetition, hour, minute);
            daoSession.insertOrReplace(alarm);
        } catch (Exception e) {
            // LOG.error("Error acquiring database", e);
        }
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public GBAlarm createFromParcel(Parcel in) {
            return readFromParcel(in);
        }

        @Override
        public GBAlarm[] newArray(int size) {
            return new GBAlarm[size];
        }

    };
}
