/*  Copyright (C) 2024 José Rebelo

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
package nodomain.freeyourgadget.gadgetbridge.devices.test;

import java.util.Random;

/**
 * Utility class to try and get deterministic random values across multiple users. This is not efficient
 * and should definitely be improved.
 */
public class TestDeviceRand {
    public static final long BASE_TIMESTAMP = 1420499943000L;

    private TestDeviceRand() {
        // utility class
    }

    public static int randInt(final long ts, final int min, final int max) {
        return new Random(ts - BASE_TIMESTAMP).nextInt((max - min) + 1) + min;
    }

    public static long randLong(final long ts, final long min, final long max) {
        return (long) (new Random(ts - BASE_TIMESTAMP).nextFloat() * (max - min) + min);
    }

    public static float randFloat(final long ts, final float min, final float max) {
        return new Random(ts - BASE_TIMESTAMP).nextFloat() * (max - min) + min;
    }

    public static boolean randBool(final long ts, final float pTrue) {
        return new Random(ts - BASE_TIMESTAMP).nextFloat() < pTrue;
    }
}
