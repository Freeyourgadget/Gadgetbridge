/*  Copyright (C) 2024 Severin von Wnuck-Lipinski

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.miscale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.Date;

import nodomain.freeyourgadget.gadgetbridge.service.btle.BLETypeConversions;

public class WeightMeasurement {
    private static final Logger LOG = LoggerFactory.getLogger(WeightMeasurement.class);

    private Date timestamp;
    private float weightKg;

    public Date getTimestamp() {
        return timestamp;
    }

    public float getWeightKg() {
        return weightKg;
    }

    private WeightMeasurement(Date timestamp, float weightKg) {
        LOG.debug("Measurement: timestamp={}, weightKg={}", timestamp, String.format("%.2f", weightKg));

        this.timestamp = timestamp;
        this.weightKg = weightKg;
    }

    public static WeightMeasurement decode(ByteBuffer buf) {
        if (buf.remaining() < 10)
            return null;

        buf.order(ByteOrder.LITTLE_ENDIAN);

        byte flags = buf.get();
        boolean stabilized = testBit(flags, 5) && !testBit(flags, 7);

        // Only decode measurement once weight reading has stabilized
        if (!stabilized)
            return null;

        float weightKg = weightToKg(buf.getShort(), flags);

        byte[] timestamp = new byte[7];
        buf.get(timestamp);
        Calendar calendar = BLETypeConversions.rawBytesToCalendar(timestamp);

        return new WeightMeasurement(calendar.getTime(), weightKg);
    }

    public static float weightToKg(float weight, byte flags) {
        boolean isLbs = testBit(flags, 0);
        boolean isJin = testBit(flags, 4);

        if (isLbs)
            return (weight / 100) * 0.45359237f;
        else if (isJin)
            return (weight / 100) * 0.5f;

        return weight / 200;
    }

    private static boolean testBit(byte value, int offset) {
        return ((value >> offset) & 1) == 1;
    }
}
