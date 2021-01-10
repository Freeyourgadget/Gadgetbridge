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

public class GetSleepDataCommand extends BaseCommand {
    public static final int SLEEP_TYPE_AWAKE = 1;
    public static final int SLEEP_TYPE_LIGHT_SLEEP = 2;
    public static final int SLEEP_TYPE_DEEP_SLEEP = 3;

    private byte daysAgo;
    private short totalRecords;
    private short currentRecord;
    private byte year;
    private byte month;
    private byte day;
    private byte hour;
    private byte minute;
    private byte sleepType;

    public byte getDaysAgo() {
        return daysAgo;
    }

    public void setDaysAgo(byte daysAgo) {
        if (daysAgo < 0 || daysAgo > 6)
            throw new IllegalArgumentException("Days ago must be between 0 and 6 inclusive");
        this.daysAgo = daysAgo;
    }

    public short getTotalRecords() {
        return totalRecords;
    }

    public short getCurrentRecord() {
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

    public byte getSleepType() {
        return sleepType;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_SLEEP_DATA, 11);

        daysAgo = params.get();
        totalRecords = params.getShort();
        currentRecord = params.getShort();
        year = params.get();
        month = params.get();
        day = params.get();
        hour = params.get();
        minute = params.get();
        sleepType = params.get();
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(daysAgo);
        return LefunConstants.CMD_SLEEP_DATA;
    }
}
