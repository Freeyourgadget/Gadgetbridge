package nodomain.freeyourgadget.gadgetbridge.service.devices.common;

import nodomain.freeyourgadget.gadgetbridge.service.btle.profiles.alertnotification.AlertCategory;

public class SimpleNotification {
    private final String message;
    private final AlertCategory alertCategory;

    public SimpleNotification(String message, AlertCategory alertCategory) {
        this.message = message;
        this.alertCategory = alertCategory;
    }

    public AlertCategory getAlertCategory() {
        return alertCategory;
    }

    public String getMessage() {
        return message;
    }
}
