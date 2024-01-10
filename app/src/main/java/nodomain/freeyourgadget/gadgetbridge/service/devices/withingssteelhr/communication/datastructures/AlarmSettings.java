/*  Copyright (C) 2023-2024 Frank Ertl

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
    along with this program.  If not, see <https://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.service.devices.withingssteelhr.communication.datastructures;

import java.nio.ByteBuffer;

public class AlarmSettings extends WithingsStructure {
    private short hour;
    private short minute;
    private short dayOfWeek;
    private short dayOfMonth;
    private short month;
    private short year;
    private short smartWakeupMinutes;

    public short getHour() {
        return hour;
    }

    public void setHour(short hour) {
        this.hour = hour;
    }

    public short getMinute() {
        return minute;
    }

    public void setMinute(short minute) {
        this.minute = minute;
    }

    public short getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(short dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public short getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(short dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public short getMonth() {
        return month;
    }

    public void setMonth(short month) {
        this.month = month;
    }

    public short getYear() {
        return year;
    }

    public void setYear(short year) {
        this.year = year;
    }

    public short getYetUnkown() {
        return smartWakeupMinutes;
    }

    public void setSmartWakeupMinutes(short smartWakeupMinutes) {
        this.smartWakeupMinutes = smartWakeupMinutes;
    }

    @Override
    public short getLength() {
        return 11;
    }

    @Override
    protected void fillinTypeSpecificData(ByteBuffer buffer) {
        buffer.put((byte)hour);
        buffer.put((byte)minute);
        buffer.put((byte)dayOfWeek);
        buffer.put((byte)dayOfMonth);
        buffer.put((byte)month);
        buffer.put((byte)year);
        buffer.put((byte)smartWakeupMinutes);
    }

    @Override
    public short getType() {
        return WithingsStructureType.ALARM;
    }

    @Override
    public boolean withEndOfMessage() {
        return true;
    }
}
