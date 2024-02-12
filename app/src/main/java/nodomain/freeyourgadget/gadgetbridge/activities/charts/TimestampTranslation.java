/*  Copyright (C) 2023-2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.activities.charts;

/**
 * Awkward class that helps in translating long timestamp
 * values to float (sic!) values. It basically rebases all
 * timestamps to a base (the very first) timestamp value.
 * <p>
 * It does this so that the large timestamp values can be used
 * floating point values, where the mantissa is just 24 bits.
 */
public class TimestampTranslation {
    private int tsOffset = -1;

    public int shorten(int timestamp) {
        if (tsOffset == -1) {
            tsOffset = timestamp;
            return 0;
        }
        return timestamp - tsOffset;
    }

    public int toOriginalValue(int timestamp) {
        if (tsOffset == -1) {
            return timestamp;
        }
        return timestamp + tsOffset;
    }

    public void reset() {
        tsOffset = -1;
    }
}
