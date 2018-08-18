/*  Copyright (C) 2016-2018 Carsten Pfeiffer, Uwe Hermann

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.heartrate;

/**
 * The Body Sensor Location characteristic of the device is used to describe the intended location of the heart rate measurement for the device.
 *
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.body_sensor_location.xml
 */
public enum BodySensorLocation {
    Other(0),
    Chest(1),
    Wrist(2),
    Finger(3),
    Hand(4),
    EarLobe(5),
    Foot(6);
    // others are reserved

    private final int val;

    BodySensorLocation(int val) {
        this.val = val;
    }
}
