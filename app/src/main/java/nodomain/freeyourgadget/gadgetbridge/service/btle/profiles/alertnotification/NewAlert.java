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

import android.icu.util.IslamicCalendar;

/**
 * https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.characteristic.new_alert.xml&u=org.bluetooth.characteristic.new_alert.xml
 *
 Recommended Usage for Text String Information Field in New Incoming Alert:

 The usage of this text is up to the implementation, but the recommended text for the category is defined as following for best user experience:

 Category: Simple Alert - The title of the alert

 Category: Email - Sender name

 Category: News - Title of the news feed

 Category: Call - Caller name or caller ID

 Category: Missed call - Caller name or caller ID

 Category: SMS - Sender name or caller ID

 Category: Voice mail - Sender name or caller ID

 Category: Schedule - Title of the schedule

 Category Hig:h Prioritized Aler - Title of the alert

 Category: Instant Messaging - Sender name
 */
public class NewAlert {
    private final AlertCategory category;
    private final int numAlerts;
    private final String message;
    private int customIcon = -1;

    public NewAlert(AlertCategory category, int /*uint8*/ numAlerts, String /*utf8s*/ message) {
        this.category = category;
        this.numAlerts = numAlerts;
        this.message = message;
    }

    public NewAlert(AlertCategory category, int /*uint8*/ numAlerts, String /*utf8s*/ message, int customIcon) {
        this.category = category;
        this.numAlerts = numAlerts;
        this.message = message;
        this.customIcon = customIcon;
    }

    public AlertCategory getCategory() {
        return category;
    }

    public int getNumAlerts() {
        return numAlerts;
    }

    public String getMessage() {
        return message;
    }

    public int getCustomIcon() {
        return customIcon;
    }
}
