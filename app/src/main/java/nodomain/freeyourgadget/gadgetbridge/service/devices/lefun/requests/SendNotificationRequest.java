/*  Copyright (C) 2020-2021 Yukai Li

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.requests;

import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.NotificationCommand;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

public class SendNotificationRequest extends AbstractSendNotificationRequest {
    NotificationSpec notification;

    public SendNotificationRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    @Override
    protected byte getNotificationType() {
        switch (notification.type) {
            case GENERIC_PHONE:
                return NotificationCommand.SERVICE_TYPE_CALL;
            case GENERIC_SMS:
            case GENERIC_EMAIL:
            default:
                return NotificationCommand.SERVICE_TYPE_TEXT;
            case WECHAT:
                return NotificationCommand.SERVICE_TYPE_WECHAT;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
            case TWITTER:
            case LINKEDIN:
            case WHATSAPP:
            case LINE:
            case KAKAO_TALK:
                return NotificationCommand.SERVICE_TYPE_EXTENDED;
        }
    }

    @Override
    protected byte getExtendedNotificationType() {
        switch (notification.type) {
            case GENERIC_PHONE:
            case GENERIC_SMS:
            case GENERIC_EMAIL:
            default:
            case WECHAT:
                return 0;
            case FACEBOOK:
            case FACEBOOK_MESSENGER:
                return NotificationCommand.EXTENDED_SERVICE_TYPE_FACEBOOK;
            case TWITTER:
                return NotificationCommand.EXTENDED_SERVICE_TYPE_TWITTER;
            case LINKEDIN:
                return NotificationCommand.EXTENDED_SERVICE_TYPE_LINKEDIN;
            case WHATSAPP:
                return NotificationCommand.EXTENDED_SERVICE_TYPE_WHATSAPP;
            case LINE:
                return NotificationCommand.EXTENDED_SERVICE_TYPE_LINE;
            case KAKAO_TALK:
                return NotificationCommand.EXTENDED_SERVICE_TYPE_KAKAOTALK;
        }
    }

    public NotificationSpec getNotification() {
        return notification;
    }

    public void setNotification(NotificationSpec notification) {
        this.notification = notification;
    }

    @Override
    protected String getMessage() {
        // Based on nodomain.freeyourgadget.gadgetbridge.service.devices.id115.SendNotificationOperation
        String message = "";

        if (notification.phoneNumber != null && !notification.phoneNumber.isEmpty()) {
            message += notification.phoneNumber + ": ";
        }

        if (notification.sender != null && !notification.sender.isEmpty()) {
            message += notification.sender + " - ";
        } else if (notification.title != null && !notification.title.isEmpty()) {
            message += notification.title + " - ";
        } else if (notification.subject != null && !notification.subject.isEmpty()) {
            message += notification.subject + " - ";
        }

        if (notification.body != null && !notification.body.isEmpty()) {
            message += notification.body;
        }

        return message;
    }
}
