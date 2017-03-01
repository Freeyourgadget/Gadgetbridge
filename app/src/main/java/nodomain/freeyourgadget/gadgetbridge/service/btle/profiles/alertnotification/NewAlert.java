package nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification;

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

    public NewAlert(AlertCategory category, int /*uint8*/ numAlerts, String /*utf8s*/ message) {
        this.category = category;
        this.numAlerts = numAlerts;
        this.message = message;
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
}
