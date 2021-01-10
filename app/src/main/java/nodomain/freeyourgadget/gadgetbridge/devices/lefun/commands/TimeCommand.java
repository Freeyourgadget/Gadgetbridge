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

public class TimeCommand extends BaseCommand {
    private byte op;
    private byte year;
    private byte month;
    private byte day;
    private byte hour;
    private byte minute;
    private byte second;

    private boolean setSuccess;

    public byte getOp() {
        if (op != OP_GET && op != OP_SET)
            throw new IllegalArgumentException("Operation must be get or set");
        return op;
    }

    public void setOp(byte op) {
        this.op = op;
    }

    public byte getYear() {
        return year;
    }

    public void setYear(byte year) {
        this.year = year;
    }

    public byte getMonth() {
        return month;
    }

    public void setMonth(byte month) {
        if (month < 1 || month > 12)
            throw new IllegalArgumentException("Month must be between 1 and 12 inclusive");
        this.month = month;
    }

    public byte getDay() {
        return day;
    }

    public void setDay(byte day) {
        if (day < 1 || day > 31)
            throw new IllegalArgumentException("Day must be between 1 and 31 inclusive");
        this.day = day;
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

    public byte getSecond() {
        return second;
    }

    public void setSecond(byte second) {
        if (second < 0 || second > 59)
            throw new IllegalArgumentException("Second must be between 0 and 59 inclusive");
        this.second = second;
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_TIME);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 1)
            throwUnexpectedLength();

        op = params.get();
        if (op == OP_GET) {
            if (paramsLength != 7)
                throwUnexpectedLength();

            year = params.get();
            month = params.get();
            day = params.get();
            hour = params.get();
            minute = params.get();
            second = params.get();
        } else if (op == OP_SET) {
            if (paramsLength != 2)
                throwUnexpectedLength();

            setSuccess = params.get() == 1;
        } else {
            throw new IllegalArgumentException("Invalid operation type received");
        }
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(op);
        if (op == OP_SET) {
            params.put(year);
            params.put(month);
            params.put(day);
            params.put(hour);
            params.put(minute);
            params.put(second);
        }
        return LefunConstants.CMD_TIME;
    }
}
