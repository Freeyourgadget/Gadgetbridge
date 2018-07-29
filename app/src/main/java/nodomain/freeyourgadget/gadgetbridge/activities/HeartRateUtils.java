/*  Copyright (C) 2016-2018 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.activities;

public class HeartRateUtils {
    public static final int MAX_HEART_RATE_VALUE = 250;
    public static final int MIN_HEART_RATE_VALUE = 10;
    /**
     * The maxiumum gap between two hr measurements in which
     * we interpolate between the measurements. Otherwise, two
     * distinct measurements will be shown.
     *
     * Value is in minutes
     */
    public static final int MAX_HR_MEASUREMENTS_GAP_MINUTES = 10;

    public static boolean isValidHeartRateValue(int value) {
        return value > HeartRateUtils.MIN_HEART_RATE_VALUE && value < HeartRateUtils.MAX_HEART_RATE_VALUE;
    }
}
