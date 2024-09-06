/*  Copyright (C) 2024 Damien Gaignon

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
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.Notifications.NotificationCapabilities;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetNotificationCapabilitiesRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetNotificationCapabilitiesRequest.class);

    public GetNotificationCapabilitiesRequest(HuaweiSupportProvider support) {
        super(support);
        this.serviceId = Notifications.id;
        this.commandId = NotificationCapabilities.id;
    }

    @Override
    protected boolean requestSupported() {
        return supportProvider.getHuaweiCoordinator().supportsPromptPushMessage() && supportProvider.getProtocolVersion() == 2;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        try {
            return new NotificationCapabilities.Request(paramsProvider).serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Get Notification Capabilities");

        if (!(receivedPacket instanceof NotificationCapabilities.Response))
            throw new ResponseTypeMismatchException(receivedPacket, NotificationCapabilities.Response.class);

        supportProvider.getHuaweiCoordinator().saveNotificationCapabilities(((NotificationCapabilities.Response) receivedPacket).capabilities);
    }
}
