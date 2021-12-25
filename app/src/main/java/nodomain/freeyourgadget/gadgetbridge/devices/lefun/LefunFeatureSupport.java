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
package nodomain.freeyourgadget.gadgetbridge.devices.lefun;

/**
 * Feature support utilities for Lefun devices
 */
public class LefunFeatureSupport {
    public static final int SUPPORT_HEART_RATE = 1 << 2;
    public static final int SUPPORT_BLOOD_PRESSURE = 1 << 3;
    public static final int SUPPORT_FAKE_ECG = 1 << 10;
    public static final int SUPPORT_ECG = 1 << 11;
    public static final int SUPPORT_WALLPAPER_UPLOAD = 1 << 12;

    public static final int RESERVE_BLOOD_OXYGEN = 1 << 0;
    public static final int RESERVE_CLOCK_FACE_UPLOAD = 1 << 3;
    public static final int RESERVE_CONTACTS = 1 << 5;
    public static final int RESERVE_WALLPAPER = 1 << 6;
    public static final int RESERVE_REMOTE_CAMERA = 1 << 7;

    /**
     * Checks whether a feature is supported
     *
     * @param deviceSupport  the feature flags from the device
     * @param featureSupport the feature you want to check
     * @return whether feature is supported
     */
    public static boolean checkSupported(short deviceSupport, int featureSupport) {
        return (deviceSupport & featureSupport) == featureSupport;
    }

    /**
     * Checks whether a feature is not reserved
     * <p>
     * Reserve flags indicate a feature is not available if set. This function takes care of the
     * inverting for you, so if you get true, the feature is available.
     *
     * @param deviceReserve  the reserve flags from the device
     * @param featureReserve the reserve flag you want to check
     * @return whether feature is supported
     */
    public static boolean checkNotReserved(short deviceReserve, int featureReserve) {
        return !((deviceReserve & featureReserve) == featureReserve);
    }
}
