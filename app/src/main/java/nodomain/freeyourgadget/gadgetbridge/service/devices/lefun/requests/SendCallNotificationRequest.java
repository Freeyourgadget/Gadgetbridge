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
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;

public class SendCallNotificationRequest extends AbstractSendNotificationRequest {
    public SendCallNotificationRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    private CallSpec callNotification;

    public CallSpec getCallNotification() {
        return callNotification;
    }

    public void setCallNotification(CallSpec callNotification) {
        this.callNotification = callNotification;
    }

    @Override
    protected String getMessage() {
        String message = "";
        if (callNotification.number != null &&!callNotification.number.isEmpty()) {
            message = callNotification.number;
        }

        if (callNotification.name != null && !callNotification.name.isEmpty()) {
            if (message.length() > 0) {
                message += " - ";
            }
            message += callNotification.name;
        }

        return message;
    }

    @Override
    protected byte getNotificationType() {
        return NotificationCommand.SERVICE_TYPE_CALL;
    }

    @Override
    protected byte getExtendedNotificationType() {
        return 0;
    }
}
