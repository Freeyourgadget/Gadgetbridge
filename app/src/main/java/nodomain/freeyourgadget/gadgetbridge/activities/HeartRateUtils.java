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

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.util.GBPrefs;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

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

    private int maxHeartRateValue;
    private int minHeartRateValue;

    private static final HeartRateUtils instance = new HeartRateUtils();

    public static HeartRateUtils getInstance() {
        return instance;
    }

    /**
     * Singleton - to access this class use the static #getInstance()
     */
    private HeartRateUtils() {
        updateCachedHeartRatePreferences();
    }

    public void updateCachedHeartRatePreferences(){
        Prefs prefs = GBApplication.getPrefs();
        maxHeartRateValue = prefs.getInt(GBPrefs.CHART_MAX_HEART_RATE, MAX_HEART_RATE_VALUE);
        minHeartRateValue = prefs.getInt(GBPrefs.CHART_MIN_HEART_RATE, MIN_HEART_RATE_VALUE);
    }

    public int getMaxHeartRate(){
        return maxHeartRateValue;
    }

    public int getMinHeartRate(){
        return minHeartRateValue;
    }

    public boolean isValidHeartRateValue(int value) {
        return value >= getMinHeartRate() && value <= getMaxHeartRate();
    }
}
