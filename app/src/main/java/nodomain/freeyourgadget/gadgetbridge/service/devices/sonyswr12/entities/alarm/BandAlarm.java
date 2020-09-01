package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.alarm;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;

public class BandAlarm {
    public static BandAlarm fromAppAlarm(Alarm alarm, int index, int interval) {
        if (!alarm.getEnabled()) return null;
        //smart wakeup = (0,10..60 min)/5
        int ahsInterval = interval / 5;
        return new BandAlarm(AlarmState.IDLE, index, ahsInterval, alarm.getHour(), alarm.getMinute(), new AlarmRepeat(alarm));
    }

    public AlarmState state;
    public int index;
    public int interval;
    public int hour;
    public int minute;
    public AlarmRepeat repeat;

    public BandAlarm(AlarmState state, int index, int interval, int hour, int minute, AlarmRepeat repeat) {
        this.state = state;
        this.index = index;
        this.interval = interval;
        this.hour = hour;
        this.minute = minute;
        this.repeat = repeat;
    }

    @Override
    public boolean equals(Object o) {
        if (this != o) {
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            BandAlarm bandAlarm = (BandAlarm) o;
            if (this.index != bandAlarm.index) {
                return false;
            }
            if (this.hour != bandAlarm.hour) {
                return false;
            }
            if (this.interval != bandAlarm.interval) {
                return false;
            }
            if (this.minute != bandAlarm.minute) {
                return false;
            }
            if (!this.repeat.equals(bandAlarm.repeat)) {
                return false;
            }
            return this.state == bandAlarm.state;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return ((((this.state.hashCode() * 31 + this.index) * 31 + this.interval) * 31 + this.hour) * 31 + this.minute) * 31 + this.repeat.hashCode();
    }
}
