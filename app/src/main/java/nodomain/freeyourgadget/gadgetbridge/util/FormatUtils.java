/*  Copyright (C) 2021-2024 Petr Vaněk

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

package nodomain.freeyourgadget.gadgetbridge.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.SettingsActivity;

public class FormatUtils {

    /**
     * This method formats a given number into a string with m (meters), km (kilometers),
     * ft (feet) or mi (miles). These string units can be translated, so the resulting text might
     * be different in each language.
     * The number is also localizes through DecimalFormatSymbols based on current locale.
     *
     * @param distance
     */
    public static String getFormattedDistanceLabel(double distance) {
        double distanceMeters = distance;
        double distanceFeet = distanceMeters * 3.28084f;
        double distanceFormatted = 0;

        String unit = GBApplication.getContext().getString(R.string.distance_format_meters);
        distanceFormatted = distanceMeters;
        if (distanceMeters > 2000) {
            distanceFormatted = distanceMeters / 1000;
            unit = GBApplication.getContext().getString(R.string.distance_format_kilometers);
        }
        String units = GBApplication.getPrefs().getString(SettingsActivity.PREF_MEASUREMENT_SYSTEM, GBApplication.getContext().getString(R.string.p_unit_metric));
        if (units.equals(GBApplication.getContext().getString(R.string.p_unit_imperial))) {
            unit = GBApplication.getContext().getString(R.string.distance_format_feet);
            distanceFormatted = distanceFeet;
            if (distanceFeet > 6000) {
                distanceFormatted = distanceFeet * 0.0001893939f;
                unit = GBApplication.getContext().getString(R.string.distance_format_miles);
            }
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(GBApplication.getLanguage());
        DecimalFormat df = new DecimalFormat(unit, symbols);

        return df.format(distanceFormatted);
    }
}