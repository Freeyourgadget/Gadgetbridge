/*  Copyright (C) 2021-2022 Gaignon Damien
    Copyright (C) 2022-2023 MartinJM

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.activities.devicesettings.DeviceSettingsPreferenceConst;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.model.CallSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationSpec;
import nodomain.freeyourgadget.gadgetbridge.model.NotificationType;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class SendNotificationRequest extends Request {

    private static final Logger LOG = LoggerFactory.getLogger(SendNotificationRequest.class);

    private HuaweiPacket packet;

    public SendNotificationRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = Notifications.NotificationActionRequest.id;
    }

    public static byte getNotificationType(NotificationType type) {
        switch (type.getGenericType()) {
            case "generic_social":
            case "generic_chat":
                return Notifications.NotificationType.weChat;
            case "generic_email":
                return Notifications.NotificationType.email;
            case "generic":
                return Notifications.NotificationType.generic;
            default:
                return Notifications.NotificationType.sms;
        }
    }


    public void buildNotificationTLVFromNotificationSpec(NotificationSpec notificationSpec) {
        String title;
        if (notificationSpec.title != null)
            title = notificationSpec.title;
        else
            title = notificationSpec.sourceName;

        this.packet = new Notifications.NotificationActionRequest(
                paramsProvider,
                supportProvider.getNotificationId(),
                getNotificationType(notificationSpec.type),
                Notifications.TextEncoding.standard,
                title,
                Notifications.TextEncoding.standard,
                notificationSpec.sender,
                Notifications.TextEncoding.standard,
                notificationSpec.body,
                notificationSpec.sourceAppId
        );
    }

    public void buildNotificationTLVFromCallSpec(CallSpec callSpec) {
        this.packet = new Notifications.NotificationActionRequest(
                paramsProvider,
                supportProvider.getNotificationId(),
                Notifications.NotificationType.call,
                Notifications.TextEncoding.standard,
                callSpec.name,
                Notifications.TextEncoding.standard,
                callSpec.name,
                Notifications.TextEncoding.standard,
                callSpec.name,
                null
        );
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return this.packet.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() {
        LOG.debug("handle Notification");
    }
}
