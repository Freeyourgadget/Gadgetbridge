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

import java.util.Arrays;

import nodomain.freeyourgadget.gadgetbridge.model.Alarm;
import nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.util.UIntBitWriter;

public class AlarmRepeat {
    private final boolean[] repeat = new boolean[7];

    public AlarmRepeat(Alarm alarm) {
        super();
        setRepeatOnDay(0, alarm.getRepetition(Alarm.ALARM_MON));
        setRepeatOnDay(1, alarm.getRepetition(Alarm.ALARM_TUE));
        setRepeatOnDay(2, alarm.getRepetition(Alarm.ALARM_WED));
        setRepeatOnDay(3, alarm.getRepetition(Alarm.ALARM_THU));
        setRepeatOnDay(4, alarm.getRepetition(Alarm.ALARM_FRI));
        setRepeatOnDay(5, alarm.getRepetition(Alarm.ALARM_SAT));
        setRepeatOnDay(6, alarm.getRepetition(Alarm.ALARM_SUN));
    }

    @Override
    public boolean equals(Object o) {
        if (this != o) {
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            return Arrays.equals(this.repeat, ((AlarmRepeat) o).repeat);
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.repeat);
    }

    public void setRepeatOnDay(int i, boolean b) {
        this.repeat[i] = b;
    }

    public int toInt() {
        UIntBitWriter uIntBitWriter = new UIntBitWriter(7);
        uIntBitWriter.appendBoolean(this.repeat[6]);
        uIntBitWriter.appendBoolean(this.repeat[5]);
        uIntBitWriter.appendBoolean(this.repeat[4]);
        uIntBitWriter.appendBoolean(this.repeat[3]);
        uIntBitWriter.appendBoolean(this.repeat[2]);
        uIntBitWriter.appendBoolean(this.repeat[1]);
        uIntBitWriter.appendBoolean(this.repeat[0]);
        return (int) uIntBitWriter.getValue();
    }
}
