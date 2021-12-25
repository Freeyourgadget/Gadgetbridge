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

public enum BandTimeZone {
    UTC_PLUS_06_30(26, 6, 30),
    UTC_PLUS_07_00(28, 7, 0),
    UTC_PLUS_08_00(32, 8, 0),
    UTC_PLUS_08_45(35, 8, 45),
    UTC_PLUS_09_00(36, 9, 0),
    UTC_PLUS_09_30(38, 9, 30),
    UTC_PLUS_10_00(40, 10, 0),
    UTC_PLUS_10_30(42, 10, 30),
    UTC_PLUS_11_00(44, 11, 0),
    UTC_PLUS_11_30(46, 11, 30),
    UTC_PLUS_12_00(48, 12, 0),
    UTC_PLUS_12_45(51, 12, 45),
    UTC_PLUS_13_00( 52, 13, 0),
    UTC_PLUS_14_00(56, 14, 0),
    UTC_MINUS_12_00(-48, -12, 0),
    UTC_MINUS_11_00(-44, -11, 0),
    UTC_MINUS_10_00(-40, -10, 0),
    UTC_MINUS_09_30(-38, -9, -30),
    UTC_MINUS_09_00(-36, -9, 0),
    UTC_MINUS_08_00(-32, -8, 0),
    UTC_MINUS_07_00(-28, -7, 0),
    UTC_MINUS_06_00(-24, -6, 0),
    UTC_MINUS_05_00(-20, -5, 0),
    UTC_MINUS_04_30(-18, -4, -30),
    UTC_MINUS_04_00(-16, -4, 0),
    UTC_MINUS_03_30(-14, -3, -30),
    UTC_MINUS_03_00(-12, -3, 0),
    UTC_MINUS_02_00(-8, -2, 0),
    UTC_MINUS_01_00(-4, -1, 0),
    UTC_PLUS_00_00(0, 0, 0),
    UTC_PLUS_01_00(4, 1, 0),
    UTC_PLUS_02_00(8, 2, 0),
    UTC_PLUS_03_00(12, 3, 0),
    UTC_PLUS_03_30(14, 3, 30),
    UTC_PLUS_04_00(16, 4, 0),
    UTC_PLUS_04_30(18, 4, 30),
    UTC_PLUS_05_00(20, 5, 0),
    UTC_PLUS_05_30(22, 5, 30),
    UTC_PLUS_05_45(23, 5, 45),
    UTC_PLUS_06_00(24, 6, 0);

    final int key;
    private final long rawOffset;

    BandTimeZone(int key, int hourOffset, int minOffset) {
        this.key = key;
        this.rawOffset = 3600000L * hourOffset + 60000L * minOffset;
    }

    public static BandTimeZone fromOffset(long rawOffset) {
        for (BandTimeZone zone : values()){
            if (zone.rawOffset == rawOffset)
                return zone;
        }
        throw new RuntimeException("wrong raw offset: " + rawOffset);
    }
}

