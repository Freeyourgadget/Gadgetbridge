/*  Copyright (C) 2017 Carsten Pfeiffer

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
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.alert_notification_control_point.xml
 */
public enum Command {
    EnableNewIncomingAlertNotification(0),
    EnableUnreadCategoryStatusNotification(1),
    DisableNewIncomingAlertNotification(2),
    DisbleUnreadCategoryStatusNotification(3),
    NotifyNewIncomingAlertImmediately(4),
    NotifyUnreadCategoryStatusImmediately(5),;
    // 6-255 reserved for future use

    private final int id;

    Command(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
