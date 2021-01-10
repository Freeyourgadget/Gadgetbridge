/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands;

import java.nio.ByteBuffer;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;

public class AlarmCommand extends BaseCommand {
    public static final int DOW_SUNDAY = 0;
    public static final int DOW_MONDAY = 1;
    public static final int DOW_TUESDAY = 2;
    public static final int DOW_WEDNESDAY = 3;
    public static final int DOW_THURSDAY = 4;
    public static final int DOW_FRIDAY = 5;
    public static final int DOW_SATURDAY = 6;

    private byte op;
    private byte index;
    private boolean enabled;
    // Snooze is not implemented how you think it would be
    // Number of snoozes is decremented every time the alarm triggers, and the alarm time
    // is moved forward by number of minutes in snooze time. It never gets reset to the
    // original time.
    private byte numOfSnoozes;
    private byte snoozeTime;
    private byte dayOfWeek;
    private byte hour;
    private byte minute;

    private boolean setSuccess;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        if (op != OP_GET && op != OP_SET)
            throw new IllegalArgumentException("Operation must be get or set");
        this.op = op;
    }

    public byte getIndex() {
        return index;
    }

    public void setIndex(byte index) {
        if (index < 0 || index >= LefunConstants.NUM_ALARM_SLOTS)
            throw new IllegalArgumentException("Index must be between 0 and "
                    + (LefunConstants.NUM_ALARM_SLOTS - 1) + " inclusive");
        this.index = index;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public byte getNumOfSnoozes() {
        return numOfSnoozes;
    }

    public void setNumOfSnoozes(byte numOfSnoozes) {
        this.numOfSnoozes = numOfSnoozes;
    }

    public byte getSnoozeTime() {
        return snoozeTime;
    }

    public void setSnoozeTime(byte snoozeTime) {
        this.snoozeTime = snoozeTime;
    }

    public boolean getDayOfWeek(int day) {
        if (day < 0 || day > 6)
            throw new IllegalArgumentException("Invalid day of week");
        return getBit(dayOfWeek, 1 << day);
    }

    public void setDayOfWeek(int day, boolean enabled) {
        if (day < 0 || day > 6)
            throw new IllegalArgumentException("Invalid day of week");
        dayOfWeek = setBit(dayOfWeek, 1 << day, enabled);
    }

    public byte getHour() {
        return hour;
    }

    public void setHour(byte hour) {
        if (hour < 0 || hour > 23)
            throw new IllegalArgumentException("Hour must be between 0 and 23 inclusive");
        this.hour = hour;
    }

    public byte getMinute() {
        return minute;
    }

    public void setMinute(byte minute) {
        if (minute < 0 || minute > 59)
            throw new IllegalArgumentException("Minute must be between 0 and 59 inclusive");
        this.minute = minute;
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_ALARM);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 2)
            throwUnexpectedLength();

        op = params.get();
        index = params.get();
        if (op == OP_GET) {
            if (paramsLength != 8)
                throwUnexpectedLength();

            enabled = params.get() == 1;
            numOfSnoozes = params.get();
            snoozeTime = params.get();
            dayOfWeek = params.get();
            hour = params.get();
            minute = params.get();
        } else if (op == OP_SET) {
            if (paramsLength != 3)
                throwUnexpectedLength();

            setSuccess = params.get() == 1;
        } else {
            throw new IllegalArgumentException("Invalid operation type received");
        }
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(op);
        // No alarm ID for get; all of them are returned
        if (op == OP_SET) {
            params.put(index);
            params.put((byte)(enabled ? 1: 0));
            params.put(numOfSnoozes);
            params.put(snoozeTime);
            params.put(dayOfWeek);
            params.put(hour);
            params.put(minute);
        }
        return LefunConstants.CMD_ALARM;
    }
}
