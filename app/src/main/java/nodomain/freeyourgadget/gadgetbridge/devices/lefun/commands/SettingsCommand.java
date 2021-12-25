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

public class SettingsCommand extends BaseCommand {
    public static final byte AM_PM_24_HOUR = 0;
    public static final byte AM_PM_12_HOUR = 1;
    public static final byte MEASUREMENT_UNIT_METRIC = 0;
    public static final byte MEASUREMENT_UNIT_IMPERIAL = 1;

    private byte op;
    private byte option1;
    private byte amPmIndicator;
    private byte measurementUnit;

    private boolean setSuccess;

    public byte getOp() {
        return op;
    }

    public void setOp(byte op) {
        if (op != OP_GET && op != OP_SET)
            throw new IllegalArgumentException("Operation must be get or set");
        this.op = op;
    }

    public byte getOption1() {
        return option1;
    }

    public void setOption1(byte option1) {
        if (option1 != (byte)0xff && (option1 < 0 || option1 > 24))
            throw new IllegalArgumentException("option1 must be between 0 and 24 inclusive");
        this.option1 = option1;
    }

    public byte getAmPmIndicator() {
        return amPmIndicator;
    }

    public void setAmPmIndicator(byte amPmIndicator) {
        if (amPmIndicator != (byte)0xff && (amPmIndicator != AM_PM_12_HOUR && amPmIndicator != AM_PM_24_HOUR))
            throw new IllegalArgumentException("Indicator must be 12 or 24 hours");
        this.amPmIndicator = amPmIndicator;
    }

    public byte getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(byte measurementUnit) {
        if (measurementUnit != (byte)0xff && (measurementUnit != MEASUREMENT_UNIT_METRIC && measurementUnit != MEASUREMENT_UNIT_IMPERIAL))
            throw new IllegalArgumentException(("Unit must be metric or imperial"));
        this.measurementUnit = measurementUnit;
    }

    public boolean isSetSuccess() {
        return setSuccess;
    }

    @Override
    protected void deserializeParams(byte id, ByteBuffer params) {
        validateId(id, LefunConstants.CMD_SETTINGS);

        int paramsLength = params.limit() - params.position();
        if (paramsLength < 1)
            throwUnexpectedLength();

        op = params.get();
        if (op == OP_GET) {
            if (paramsLength != 4)
                throwUnexpectedLength();

            option1 = params.get();
            amPmIndicator = params.get();
            measurementUnit = params.get();
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
            params.put(option1);
            params.put(amPmIndicator);
            params.put(measurementUnit);
        }
        return LefunConstants.CMD_SETTINGS;
    }
}
