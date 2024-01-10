/*  Copyright (C) 2022-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huami;

import java.math.BigDecimal;
import java.math.RoundingMode;

import nodomain.freeyourgadget.gadgetbridge.GBException;
import nodomain.freeyourgadget.gadgetbridge.entities.BaseActivitySummary;
import nodomain.freeyourgadget.gadgetbridge.model.ActivityTrack;
import nodomain.freeyourgadget.gadgetbridge.model.GPSCoordinate;

public abstract class AbstractHuamiActivityDetailsParser {
    private static final BigDecimal HUAMI_TO_DECIMAL_DEGREES_DIVISOR = new BigDecimal("3000000.0");

    public abstract ActivityTrack parse(final byte[] bytes) throws GBException;

    public static double convertHuamiValueToDecimalDegrees(final long huamiValue) {
        BigDecimal result = new BigDecimal(huamiValue)
                .divide(HUAMI_TO_DECIMAL_DEGREES_DIVISOR, GPSCoordinate.GPS_DECIMAL_DEGREES_SCALE, RoundingMode.HALF_UP);
        return result.doubleValue();
    }

    protected static String createActivityName(final BaseActivitySummary summary) {
        String name = summary.getName();
        String nameText = "";
        Long id = summary.getId();
        if (name != null) {
            nameText = name + " - ";
        }
        return nameText + id;
    }
}
