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

public class GetActivityDataCommand extends BaseCommand {
    private byte daysAgo;
    private byte totalRecords;
    private byte currentRecord;
    private byte year;
    private byte month;
    private byte day;
    private byte hour;
    private byte minute;
    private short steps;
    private short distance; // m
    private short calories; // calories

    public byte getDaysAgo() {
        return daysAgo;
    }

    public void setDaysAgo(byte daysAgo) {
        if (daysAgo < 0 || daysAgo > 6)
            throw new IllegalArgumentException("Days ago must be between 0 and 6 inclusive");
        this.daysAgo = daysAgo;
    }

    public byte getTotalRecords() {
        return totalRecords;
    }

    public byte getCurrentRecord() {
        return currentRecord;
    }

    public byte getYear() {
        return year;
    }

    public byte getMonth() {
        return month;
    }

    public byte getDay() {
        return day;
    }

    public byte getHour() {
        return hour;
    }

    public byte getMinute() {
        return minute;
    }

    public short getSteps() {
        return steps;
    }

    public short getDistance() {
        return distance;
    }

    public short getCalories() {
        return calories;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_ACTIVITY_DATA, 14);

        daysAgo = params.get();
        totalRecords = params.get();
        currentRecord = params.get();
        year = params.get();
        month = params.get();
        day = params.get();
        hour = params.get();
        minute = params.get();
        steps = params.getShort();
        distance = params.getShort();
        calories = params.getShort();
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(daysAgo);
        return LefunConstants.CMD_ACTIVITY_DATA;
    }
}
