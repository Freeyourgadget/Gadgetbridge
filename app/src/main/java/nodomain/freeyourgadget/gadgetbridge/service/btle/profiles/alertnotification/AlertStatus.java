/*  Copyright (C) 2016-2019 Carsten Pfeiffer

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
package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_status.xml
 * uint8 value (bitmask) of the given values
 */
public class AlertStatus {
    public static final int RINGER_ACTIVE_BIT = 1;
    public static final int VIBRATE_ACTIVE = 1 << 1;
    public static final int DISPLAY_ALERT_ACTIVE = 1 << 2;

    public static boolean isRingerActive(int status) {
        return (status & RINGER_ACTIVE_BIT) == RINGER_ACTIVE_BIT;
    }
    public static boolean isVibrateActive(int status) {
        return (status & VIBRATE_ACTIVE) == VIBRATE_ACTIVE;
    }
    public static boolean isDisplayAlertActive(int status) {
        return (status & DISPLAY_ALERT_ACTIVE) == DISPLAY_ALERT_ACTIVE;
    }
}
