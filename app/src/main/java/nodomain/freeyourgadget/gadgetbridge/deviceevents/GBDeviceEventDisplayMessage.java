/*  Copyright (C) 2016-2017 Andreas Shimokawa, Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventDisplayMessage {
    public String message;
    public int duration;
    public int severity;

    /**
     * An event for displaying a message to the user. How the message is displayed
     * is a detail of the current activity, which needs to listen to the Intent
     * GB.ACTION_DISPLAY_MESSAGE.
     *
     * @param message
     * @param duration
     * @param severity
     */
    public GBDeviceEventDisplayMessage(String message, int duration, int severity) {
        this.message = message;
        this.duration = duration;
        this.severity = severity;
    }
}
