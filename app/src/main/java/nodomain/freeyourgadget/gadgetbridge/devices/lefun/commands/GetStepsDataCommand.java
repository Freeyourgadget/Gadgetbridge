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

public class GetStepsDataCommand extends BaseCommand {
    private byte daysAgo;
    private byte year;
    private byte month;
    private byte day;
    private int steps;
    private int distance; // m
    private int calories; // calories

    public byte getDaysAgo() {
        return daysAgo;
    }

    public void setDaysAgo(byte daysAgo) {
        if (daysAgo < 0 || daysAgo > 6)
            throw new IllegalArgumentException("Days ago must be between 0 and 6 inclusive");
        this.daysAgo = daysAgo;
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

    public int getSteps() {
        return steps;
    }

    public int getDistance() {
        return distance;
    }

    public int getCalories() {
        return calories;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateIdAndLength(id, params, LefunConstants.CMD_STEPS_DATA, 16);

        daysAgo = params.get();
        year = params.get();
        month = params.get();
        day = params.get();
        steps = params.getInt();
        distance = params.getInt();
        calories = params.getInt();
    }

    @Override
    protected byte serializeParams(ByteBuffer params) {
        params.put(daysAgo);
        return LefunConstants.CMD_STEPS_DATA;
    }
}
