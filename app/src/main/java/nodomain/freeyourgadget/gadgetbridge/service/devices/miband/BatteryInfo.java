/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miband;

import java.util.GregorianCalendar;

import nodomain.freeyourgadget.gadgetbridge.devices.miband.MiBandDateConverter;
import nodomain.freeyourgadget.gadgetbridge.model.BatteryState;

public class BatteryInfo extends AbstractInfo {
    public static final byte DEVICE_BATTERY_NORMAL = 0;
    public static final byte DEVICE_BATTERY_LOW = 1;
    public static final byte DEVICE_BATTERY_CHARGING = 2;
    public static final byte DEVICE_BATTERY_CHARGING_FULL = 3;
    public static final byte DEVICE_BATTERY_CHARGE_OFF = 4;

    public BatteryInfo(byte[] data) {
        super(data);
    }

    public int getLevelInPercent() {
        if (mData.length >= 1) {
            return mData[0];
        }
        return 50; // actually unknown
    }

    public BatteryState getState() {
        if (mData.length >= 10) {
            int value = mData[9];
            switch (value) {
                case DEVICE_BATTERY_NORMAL:
                    return BatteryState.BATTERY_NORMAL;
                case DEVICE_BATTERY_LOW:
                    return BatteryState.BATTERY_LOW;
                case DEVICE_BATTERY_CHARGING:
                    return BatteryState.BATTERY_CHARGING;
                case DEVICE_BATTERY_CHARGING_FULL:
                    return BatteryState.BATTERY_CHARGING_FULL;
                case DEVICE_BATTERY_CHARGE_OFF:
                    return BatteryState.BATTERY_NOT_CHARGING_FULL;
            }
        }
        return BatteryState.UNKNOWN;
    }

    public GregorianCalendar getLastChargeTime() {
        GregorianCalendar lastCharge = MiBandDateConverter.createCalendar();

        if (mData.length >= 10) {
            lastCharge = MiBandDateConverter.rawBytesToCalendar(new byte[]{
                    mData[1], mData[2], mData[3], mData[4], mData[5], mData[6]
            });
        }

        return lastCharge;
    }

    public int getNumCharges() {
        if (mData.length >= 10) {
            return ((0xff & mData[7]) | ((0xff & mData[8]) << 8));

        }
        return -1;
    }
}
