/*  Copyright (C) 2020-2021 opavlov

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
