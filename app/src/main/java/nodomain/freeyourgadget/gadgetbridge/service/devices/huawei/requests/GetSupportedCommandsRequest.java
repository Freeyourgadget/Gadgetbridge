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

/* In order to be compatible with all devices, request send all possible commands
to all possible services. This implies long packet which is not handled on the device.
Thus, this request could be sliced in 3 packets. But this command does not support slicing.
Thus, one need to send multiple requests and concat the response.
Packets should be 240 bytes max */
package nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import nodomain.freeyourgadget.gadgetbridge.devices.huawei.HuaweiPacket;
import nodomain.freeyourgadget.gadgetbridge.devices.huawei.packets.DeviceConfig;
import nodomain.freeyourgadget.gadgetbridge.service.devices.huawei.HuaweiSupportProvider;

public class GetSupportedCommandsRequest extends Request {
    private static final Logger LOG = LoggerFactory.getLogger(GetSupportedCommandsRequest.class);

    private final Map<Integer, byte[]> commandsPerService;
    private final List<Byte> activatedServices;

    public GetSupportedCommandsRequest(
            HuaweiSupportProvider support,
            List<Byte> activatedServices
    ) {
        super(support);
        this.serviceId = DeviceConfig.id;
        this.commandId = DeviceConfig.SupportedCommands.id;
        this.commandsPerService = DeviceConfig.SupportedCommands.commandsPerService;
        this.activatedServices = activatedServices;
    }

    @Override
    protected List<byte[]> createRequest() throws RequestCreationException {
        DeviceConfig.SupportedCommands.Request commandsRequest = new DeviceConfig.SupportedCommands.Request(paramsProvider);
        byte nextService = activatedServices.remove(0);
        boolean fits = commandsRequest.addCommandsForService(nextService, this.commandsPerService.get((int) nextService));
        while (fits && activatedServices.size() > 0) {
            nextService = activatedServices.remove(0);
            fits = commandsRequest.addCommandsForService(nextService, this.commandsPerService.get((int) nextService));
        }
        if (!fits)
            activatedServices.add(0, nextService); // Put the extra back
        try {
            return commandsRequest.serialize();
        } catch (HuaweiPacket.CryptoException e) {
            throw new RequestCreationException(e);
        }
    }

    RequestCallback dynamicServicesReq = new RequestCallback() {
        @Override
        public void call() {
            supportProvider.initializeDynamicServices();
        }
    };

    @Override
    protected void processResponse() throws ResponseParseException {
        LOG.debug("handle Supported Commands");

        if (!(receivedPacket instanceof DeviceConfig.SupportedCommands.Response))
            throw new ResponseTypeMismatchException(receivedPacket, DeviceConfig.SupportedCommands.Response.class);

        for (DeviceConfig.SupportedCommands.Response.CommandsList commandsList : ((DeviceConfig.SupportedCommands.Response) receivedPacket).commandsLists) {
            supportProvider.getHuaweiCoordinator().addCommandsForService(
                    commandsList.service,
                    commandsList.commands
            );
        }

        if (activatedServices.size() > 0) {
            GetSupportedCommandsRequest nextRequest = new GetSupportedCommandsRequest(supportProvider, activatedServices);
            this.nextRequest(nextRequest);
        } else {
            supportProvider.getHuaweiCoordinator().printCommandsPerService();
            if (supportProvider.getHuaweiCoordinator().supportsExpandCapability()) {
                GetExpandCapabilityRequest nextRequest = new GetExpandCapabilityRequest(supportProvider);
                nextRequest.setFinalizeReq(dynamicServicesReq);
                this.nextRequest(nextRequest);
            } else {
                dynamicServicesReq.call();
            }
        }
    }
}
