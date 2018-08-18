/*  Copyright (C) 2015-2018 Andreas Shimokawa, Carsten Pfeiffer, Daniele
    Gobbetti, Szymon Tomasz Stefanek

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
package nodomain.freeyourgadget.gadgetbridge.devices.miband;

import java.util.Calendar;
import java.util.GregorianCalendar;

public class MiBandDateConverter {
    /**
     * Creates a calendar object representing the current date and time.
     */
    public static GregorianCalendar createCalendar() {
        return new GregorianCalendar();
    }

    /**
     * uses the standard algorithm to convert bytes received from the MiBand to a Calendar object
     *
     * @param value
     * @return
     */
    public static GregorianCalendar rawBytesToCalendar(byte[] value) {
        if (value.length == 6) {
            return rawBytesToCalendar(value, 0);
        }
        return createCalendar();
    }

    /**
     * uses the standard algorithm to convert bytes received from the MiBand to a Calendar object
     *
     * @param value
     * @return
     */
    public static GregorianCalendar rawBytesToCalendar(byte[] value, int offset) {
        if (value.length - offset >= 6) {
            GregorianCalendar timestamp = new GregorianCalendar(
                    value[offset] + 2000,
                    value[offset + 1],
                    value[offset + 2],
                    value[offset + 3],
                    value[offset + 4],
                    value[offset + 5]);

	        int offsetInHours = MiBandCoordinator.getDeviceTimeOffsetHours();
	        if(offsetInHours != 0)
		        timestamp.add(Calendar.HOUR_OF_DAY,-offsetInHours);

            return timestamp;
        }

        return createCalendar();
    }

    /**
     * uses the standard algorithm to convert a Calendar object to a byte array to send to MiBand
     *
     * @param timestamp
     * @return
     */
    public static byte[] calendarToRawBytes(Calendar timestamp) {

	    // The mi-band device currently records sleep
	    // only if it happens after 10pm and before 7am.
	    // The offset is used to trick the device to record sleep
	    // in non-standard hours.
	    // If you usually sleep, say, from 6am to 2pm, set the
	    // shift to -8, so at 6am the device thinks it's still 10pm
	    // of the day before.
	    int offsetInHours = MiBandCoordinator.getDeviceTimeOffsetHours();
	    if(offsetInHours != 0)
			timestamp.add(Calendar.HOUR_OF_DAY,offsetInHours);

        return new byte[]{
                (byte) (timestamp.get(Calendar.YEAR) - 2000),
                (byte) timestamp.get(Calendar.MONTH),
                (byte) timestamp.get(Calendar.DATE),
                (byte) timestamp.get(Calendar.HOUR_OF_DAY),
                (byte) timestamp.get(Calendar.MINUTE),
                (byte) timestamp.get(Calendar.SECOND)
        };
    }
}
