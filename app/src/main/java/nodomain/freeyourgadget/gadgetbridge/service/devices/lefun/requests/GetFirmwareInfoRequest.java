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

import nodomain.freeyourgadget.gadgetbridge.deviceevents.GBDeviceEventVersionInfo;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.LefunConstants;
import nodomain.freeyourgadget.gadgetbridge.devices.lefun.commands.GetFirmwareInfoCommand;
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice;
import nodomain.freeyourgadget.gadgetbridge.service.btle.TransactionBuilder;
import nodomain.freeyourgadget.gadgetbridge.service.devices.lefun.LefunDeviceSupport;
import nodomain.freeyourgadget.gadgetbridge.service.devices.miband.operations.OperationStatus;

public class GetFirmwareInfoRequest extends Request {
    public GetFirmwareInfoRequest(LefunDeviceSupport support, TransactionBuilder builder) {
        super(support, builder);
    }

    @Override
    public byte[] createRequest() {
        GetFirmwareInfoCommand cmd = new GetFirmwareInfoCommand();
        return cmd.serialize();
    }

    @Override
    public void handleResponse(byte[] data) {
        GetFirmwareInfoCommand cmd = new GetFirmwareInfoCommand();
        cmd.deserialize(data);

        int hardwareVersion = cmd.getHardwareVersion() & 0xffff;
        int softwareVersion = cmd.getSoftwareVersion() & 0xffff;

        GBDeviceEventVersionInfo versionInfo = new GBDeviceEventVersionInfo();
        versionInfo.fwVersion = String.format("%d.%d", softwareVersion >> 8, softwareVersion & 0xff);
        // Last character is a \x1f? Not printable either way.
        versionInfo.hwVersion = cmd.getTypeCode().substring(0, 3);
        getSupport().evaluateGBDeviceEvent(versionInfo);

        GBDevice device = getSupport().getDevice();
        device.setFirmwareVersion2(String.format("%d.%d", hardwareVersion >> 8, hardwareVersion & 0xff));

        getSupport().completeInitialization();

        operationStatus = OperationStatus.FINISHED;
    }

    @Override
    public int getCommandId() {
        return LefunConstants.CMD_FIRMWARE_INFO;
    }
}
