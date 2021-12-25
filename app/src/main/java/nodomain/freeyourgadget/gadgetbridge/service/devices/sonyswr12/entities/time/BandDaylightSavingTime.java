/*  Copyright (C) 2020-2021 opavlov

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.sonyswr12.entities.time;

public enum BandDaylightSavingTime {
    STANDARD_TIME(0, 0),
    HALF_AN_HOUR_DST(2, 30),
    DST(4, 60),
    DOUBLE_DST( 8, 120);

    final int key;
    private final long saving;

    BandDaylightSavingTime(int key, int min) {
        this.key = key;
        this.saving = 60000L * min;
    }

    public static BandDaylightSavingTime fromOffset(final int dstSaving) {
        for (BandDaylightSavingTime dst: values()){
            if (dst.saving == dstSaving)
                return dst;
        }
        throw new RuntimeException("wrong dst saving: " + dstSaving);
    }
}
