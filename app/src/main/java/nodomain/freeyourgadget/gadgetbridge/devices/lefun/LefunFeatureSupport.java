/*  Copyright (C) 2016-2020 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti
    Copyright (C) 2020 Yukai Li

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

    public static boolean checkSupported(short deviceSupport, int featureSupport) {
        return (deviceSupport & featureSupport) == featureSupport;
    }

    public static boolean checkNotReserved(short deviceReserve, int featureReserve) {
        return !((deviceReserve & featureReserve) == featureReserve);
    }
}
