/*  Copyright (C) 2016-2017 Andreas Shimokawa, Daniele Gobbetti

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
package nodomain.freeyourgadget.gadgetbridge.util;

public class PebbleUtils {
    public static String getPlatformName(String hwRev) {
        String platformName;
        if (hwRev.startsWith("snowy")) {
            platformName = "basalt";
        } else if (hwRev.startsWith("spalding")) {
            platformName = "chalk";
        } else if (hwRev.startsWith("silk")) {
            platformName = "diorite";
        } else if (hwRev.startsWith("robert")) {
            platformName = "emery";
        } else {
            platformName = "aplite";
        }
        return platformName;
    }

    public static String getModel(String hwRev) {
        //TODO: get real data?
        String model;
        if (hwRev.startsWith("snowy")) {
            model = "pebble_time_black";
        } else if (hwRev.startsWith("spalding")) {
            model = "pebble_time_round_black_20mm";
        } else if (hwRev.startsWith("silk")) {
            model = "pebble2_black";
        } else if (hwRev.startsWith("robert")) {
            model = "pebble_time2_black";
        } else {
            model = "pebble_black";
        }
        return model;
    }

    public static int getFwMajor(String fwString) {
        return fwString.charAt(1) - 48;
    }

    public static boolean hasHRM(String hwRev) {
        String platformName = getPlatformName(hwRev);
        return "diorite".equals(platformName) || "emery".equals(platformName);
    }

    public static boolean hasHealth(String hwRev) {
        String platformName = getPlatformName(hwRev);
        return !"aplite".equals(platformName);
    }
}
