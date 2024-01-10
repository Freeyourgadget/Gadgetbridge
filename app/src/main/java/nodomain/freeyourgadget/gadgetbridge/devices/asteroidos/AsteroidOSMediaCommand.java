/*  Copyright (C) 2022-2024 Noodlez

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
package nodomain.freeyourgadget.gadgetbridge.devices.asteroidos;

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventMusicControl;

/**
 * An adapter class for the media commands sent by AsteroidOS
 */
public class AsteroidOSMediaCommand {
    public static final byte COMMAND_PREVIOUS = 0x0;
    public static final byte COMMAND_NEXT = 0x1;
    public static final byte COMMAND_PLAY = 0x2;
    public static final byte COMMAND_PAUSE = 0x3;
    public static final byte COMMAND_VOLUME = 0x4;

    public byte command;
    public AsteroidOSMediaCommand(byte value) {
        command = value;
    }

    /**
     * Convert the MediaCommand to a music control event
     * @return the matching music control event
     */
    public GBDeviceEventMusicControl toMusicControlEvent() {
        GBDeviceEventMusicControl event = new GBDeviceEventMusicControl();
        switch (command) {
            case COMMAND_PREVIOUS:
                event.event = GBDeviceEventMusicControl.Event.PREVIOUS;
                break;
            case COMMAND_NEXT:
                event.event = GBDeviceEventMusicControl.Event.NEXT;
                break;
            case COMMAND_PLAY:
                event.event = GBDeviceEventMusicControl.Event.PLAY;
                break;
            case COMMAND_PAUSE:
                event.event = GBDeviceEventMusicControl.Event.PAUSE;
                break;
            case COMMAND_VOLUME:
            default:
                event.event = GBDeviceEventMusicControl.Event.UNKNOWN;
        }
        return event;
    }
}
