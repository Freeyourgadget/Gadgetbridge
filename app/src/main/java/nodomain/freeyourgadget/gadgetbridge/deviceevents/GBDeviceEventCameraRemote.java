/*  Copyright (C) 2024 Martin.JM

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
package nodomain.freeyourgadget.gadgetbridge.deviceevents;

public class GBDeviceEventCameraRemote extends GBDeviceEvent {
    public Event event = Event.UNKNOWN;

    public enum Event {
        UNKNOWN,
        OPEN_CAMERA,
        TAKE_PICTURE,
        CLOSE_CAMERA,
        EXCEPTION
    }

    static public int eventToInt(Event event) {
        switch (event) {
            case UNKNOWN:
                return 0;
            case OPEN_CAMERA:
                return 1;
            case TAKE_PICTURE:
                return 2;
            case CLOSE_CAMERA:
                return 3;
        }
        return -1;
    }

    static public Event intToEvent(int event) {
        switch (event) {
            case 0:
                return Event.UNKNOWN;
            case 1:
                return Event.OPEN_CAMERA;
            case 2:
                return Event.TAKE_PICTURE;
            case 3:
                return Event.CLOSE_CAMERA;
        }
        return Event.EXCEPTION;
    }
}
